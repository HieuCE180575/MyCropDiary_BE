package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.admin.ChecklistRuleRespDTO;
import com.mycropdiary.api.dto.admin.UpdateChecklistRuleReqDTO;
import com.mycropdiary.api.dto.vietgap.*;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.*;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.repository.cultivation.*;
import com.mycropdiary.api.service.admin.impl.ChecklistRuleServiceImpl;
import com.mycropdiary.api.service.impl.InternalAssessmentServiceImpl;
import com.mycropdiary.api.service.impl.ChecklistReviewServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VietGAP Checklist & Internal Assessment Service Unit Tests")
public class FE09_VietGapChecklistAndInternalAssessmentUnitTest {

    @Mock
    private ChecklistRuleRepository checklistRuleRepository;
    @Mock
    private ChecklistReviewRepository checklistRunRepository;
    @Mock
    private ChecklistResultRepository checklistResultRepository;
    @Mock
    private InternalAssessmentRepository assessmentRepository;
    @Mock
    private InternalAssessmentItemRepository itemRepository;
    @Mock
    private AssessmentCriterionRepository criterionRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private CropSeasonRepository cropSeasonRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private FarmMemberRepository farmMemberRepository;
    @Mock
    private FarmAccessGuard accessGuard;

    @InjectMocks
    private ChecklistReviewServiceImpl checklistService;

    @InjectMocks
    private InternalAssessmentServiceImpl assessmentService;

    private Farm farm;
    private User user;
    private FarmMember member;
    private ChecklistRule checklistRule;
    private AssessmentCriterion criterion1;
    private AssessmentCriterion criterion2;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(1);

        user = new User("inspector@vietgap.com", "passhash", "VietGAP Inspector", "0912345678", "user", "active");
        user.setId(10);

        member = new FarmMember(farm, user, "owner", "active");
        member.setId(2);

        checklistRule = new ChecklistRule(farm, user, "Pre-Harvest Interval (PHI) Quarantine Check", "Inspect quarantine logbook", "VietGAP Food Safety", true);
        checklistRule.setId(100);

        criterion1 = new AssessmentCriterion(farm, "TCVN 1: Khu vực sản xuất", "Đánh giá nguy cơ ô nhiễm hóa chất", new BigDecimal("100.00"), true);
        criterion1.setId(1);

        criterion2 = new AssessmentCriterion(farm, "TCVN 2: Phân bón và chất bổ sung", "Tuân thủ danh mục phân bón cho phép", new BigDecimal("100.00"), true);
        criterion2.setId(2);
    }

    @Test
    @DisplayName("Run VietGAP Evidence Checklist: Should succeed with valid inputs and items")
    void runChecklist_validInputs_shouldSucceed() {
        when(accessGuard.requireFarmAccess(10, 1)).thenReturn(member);
        when(checklistRuleRepository.findById(100)).thenReturn(Optional.of(checklistRule));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 10, "active")).thenReturn(Optional.of(member));

        ChecklistRun savedRun = new ChecklistRun(checklistRule, null, null, member, "completed", Instant.now(), Instant.now());
        savedRun.setId(501);
        when(checklistRunRepository.save(any(ChecklistRun.class))).thenReturn(savedRun);

        ChecklistResult savedResult = new ChecklistResult(savedRun, member, "pass", "14 days quarantine satisfied", "https://img.com/evidence1.jpg", Instant.now());
        savedResult.setId(601);
        when(checklistResultRepository.save(any(ChecklistResult.class))).thenReturn(savedResult);

        RunChecklistReqDTO req = new RunChecklistReqDTO(
                100, null, null, null, null,
                "Check before harvesting tomato crop",
                List.of(new SubmitChecklistItemReqDTO("pass", "14 days quarantine satisfied", "https://img.com/evidence1.jpg"))
        );

        ChecklistResultRespDTO resp = checklistService.runChecklist(10, 1, req);
        assertNotNull(resp);
        assertEquals(501, resp.runId());
        assertEquals("Pre-Harvest Interval (PHI) Quarantine Check", resp.ruleName());
        assertEquals(1, resp.totalItems());
        assertEquals(1, resp.passedItems());
        assertEquals(0, resp.failedItems());
        assertEquals(0, resp.missingEvidenceCount());
    }

    @Test
    @DisplayName("Run VietGAP Evidence Checklist: Should throw BadRequest on invalid result status")
    void runChecklist_invalidResultStatus_shouldThrowBadRequest() {
        when(accessGuard.requireFarmAccess(10, 1)).thenReturn(member);
        when(checklistRuleRepository.findById(100)).thenReturn(Optional.of(checklistRule));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 10, "active")).thenReturn(Optional.of(member));

        ChecklistRun savedRun = new ChecklistRun(checklistRule, null, null, member, "completed", Instant.now(), Instant.now());
        savedRun.setId(501);
        when(checklistRunRepository.save(any(ChecklistRun.class))).thenReturn(savedRun);

        RunChecklistReqDTO req = new RunChecklistReqDTO(
                100, null, null, null, null, "Check",
                List.of(new SubmitChecklistItemReqDTO("UNKNOWN_STATUS", "Note", null))
        );

        assertThrows(BadRequestException.class, () -> checklistService.runChecklist(10, 1, req));
    }

    @Test
    @DisplayName("View Checklist Item Details: Should flag missing evidence correctly")
    void getChecklistItemDetails_shouldFlagMissingEvidence() {
        ChecklistRun run = new ChecklistRun(checklistRule, null, null, member, "completed", Instant.now(), Instant.now());
        run.setId(501);
        when(checklistRunRepository.findByIdWithDetails(501)).thenReturn(Optional.of(run));
        when(accessGuard.requireFarmAccess(10, 1)).thenReturn(member);

        ChecklistResult r1 = new ChecklistResult(run, member, "pass", "Checked", null, Instant.now());
        r1.setId(1); // missing image URL -> isMissingEvidence should be true
        ChecklistResult r2 = new ChecklistResult(run, member, "pass", "Checked with photo", "https://img.com/pic.jpg", Instant.now());
        r2.setId(2); // has image URL -> isMissingEvidence should be false

        when(checklistResultRepository.findByRunIdWithDetails(501)).thenReturn(List.of(r1, r2));

        List<ChecklistResultItemRespDTO> items = checklistService.getChecklistItemDetails(10, 501);
        assertEquals(2, items.size());
        assertTrue(items.get(0).isMissingEvidence());
        assertFalse(items.get(1).isMissingEvidence());
    }

    @Test
    @DisplayName("Conduct Internal VietGAP Assessment: Should calculate total score and compliance status correctly")
    void conductInternalAssessment_validInputs_shouldCalculateCompliance() {
        when(accessGuard.requireFarmAccess(10, 1)).thenReturn(member);
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 10, "active")).thenReturn(Optional.of(member));

        InternalAssessment savedAssessment = new InternalAssessment(
                farm, null, null, member, "Quarterly Internal Audit Q1", Instant.now(), "Overall good adherence", null
        );
        savedAssessment.setId(701);
        when(assessmentRepository.save(any(InternalAssessment.class))).thenReturn(savedAssessment);

        when(criterionRepository.findById(1)).thenReturn(Optional.of(criterion1));
        when(criterionRepository.findById(2)).thenReturn(Optional.of(criterion2));

        InternalAssessmentItem item1 = new InternalAssessmentItem(savedAssessment, criterion1, new BigDecimal("90.00"), "Safe distance to industry", "https://pic.com/1.jpg");
        item1.setId(801);
        InternalAssessmentItem item2 = new InternalAssessmentItem(savedAssessment, criterion2, new BigDecimal("85.00"), "Organic compost certified", "https://pic.com/2.jpg");
        item2.setId(802);

        when(itemRepository.save(any(InternalAssessmentItem.class))).thenReturn(item1).thenReturn(item2);

        ConductAssessmentReqDTO req = new ConductAssessmentReqDTO(
                "Quarterly Internal Audit Q1", null, null, Instant.now(), "Overall good adherence", null,
                List.of(
                        new InternalAssessmentItemReqDTO(1, new BigDecimal("90.00"), "Safe distance to industry", "https://pic.com/1.jpg"),
                        new InternalAssessmentItemReqDTO(2, new BigDecimal("85.00"), "Organic compost certified", "https://pic.com/2.jpg")
                )
        );

        AssessmentRespDTO resp = assessmentService.conductInternalAssessment(10, 1, req);
        assertNotNull(resp);
        assertEquals(701, resp.internalAssessmentId());
        assertEquals("COMPLIANT", resp.complianceStatus());
        assertEquals(new BigDecimal("175.00"), resp.totalScore());
        assertEquals(new BigDecimal("200.00"), resp.maxPossibleScore());
        assertEquals(new BigDecimal("87.50"), resp.scorePercentage());
        assertEquals(2, resp.totalCriteria());
        assertEquals(2, resp.criteriaWithEvidence());
    }

    @Test
    @DisplayName("Conduct Internal VietGAP Assessment: Should reject score exceeding criterion maxScore")
    void conductInternalAssessment_scoreExceedsMaxScore_shouldThrowBadRequest() {
        when(accessGuard.requireFarmAccess(10, 1)).thenReturn(member);
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 10, "active")).thenReturn(Optional.of(member));

        InternalAssessment savedAssessment = new InternalAssessment(
                farm, null, null, member, "Audit", Instant.now(), null, null
        );
        savedAssessment.setId(701);
        when(assessmentRepository.save(any(InternalAssessment.class))).thenReturn(savedAssessment);
        when(criterionRepository.findById(1)).thenReturn(Optional.of(criterion1));

        ConductAssessmentReqDTO req = new ConductAssessmentReqDTO(
                "Audit", null, null, Instant.now(), null, null,
                List.of(new InternalAssessmentItemReqDTO(1, new BigDecimal("150.00"), "Over max score", null))
        );

        assertThrows(BadRequestException.class, () -> assessmentService.conductInternalAssessment(10, 1, req));
    }

    @Test
    @DisplayName("FE-09 Checklist Version Preservation: V1 run stays locked with V1 snapshot after Admin updates criteria to V2; reassessment creates separate V2 run")
    void checklistVersionLifecycle_v1Completed_adminUpdatesToV2_reassessmentCreatesSeparateRun() {
        CropSeason season = new CropSeason();
        season.setId(20);
        season.setFarm(farm);
        season.setSeasonName("Autumn-Winter 2026");

        ChecklistRule masterRule = new ChecklistRule(
                null, user, "VG-100", "Initial V1 Criterion", "Original V1 description",
                "RECORDS_AND_TRAINING", "MUST", "ALL", true
        );
        masterRule.setId(300);
        masterRule.setVersionNumber(1);

        ChecklistRuleServiceImpl ruleService = new ChecklistRuleServiceImpl(checklistRuleRepository);

        when(accessGuard.requireFarmAccess(10, 1)).thenReturn(member);
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(cropSeasonRepository.findById(20)).thenReturn(Optional.of(season));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 10, "active")).thenReturn(Optional.of(member));
        when(checklistRuleRepository.findSystemMasterRules()).thenReturn(List.of(masterRule));
        when(checklistRuleRepository.findById(300)).thenReturn(Optional.of(masterRule));
        when(checklistRuleRepository.save(any(ChecklistRule.class))).thenAnswer(inv -> inv.getArgument(0));
        when(checklistRuleRepository.findMaxSystemMasterVersion()).thenAnswer(inv -> masterRule.getVersionNumber());

        java.util.concurrent.atomic.AtomicInteger runSeq = new java.util.concurrent.atomic.AtomicInteger(900);
        java.util.concurrent.atomic.AtomicInteger resultSeq = new java.util.concurrent.atomic.AtomicInteger(1000);
        java.util.Map<Integer, ChecklistRun> runStore = new java.util.HashMap<>();
        java.util.Map<Integer, java.util.List<ChecklistResult>> resultStore = new java.util.HashMap<>();

        when(checklistRunRepository.save(any(ChecklistRun.class))).thenAnswer(inv -> {
            ChecklistRun r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(runSeq.incrementAndGet());
            }
            runStore.put(r.getId(), r);
            return r;
        });
        when(checklistRunRepository.findByIdWithDetails(anyInt())).thenAnswer(inv ->
                Optional.ofNullable(runStore.get(inv.getArgument(0, Integer.class)))
        );
        when(checklistResultRepository.save(any(ChecklistResult.class))).thenAnswer(inv -> {
            ChecklistResult res = inv.getArgument(0);
            if (res.getId() == null) {
                res.setId(resultSeq.incrementAndGet());
            }
            resultStore.computeIfAbsent(res.getChecklistRun().getId(), k -> new java.util.ArrayList<>());
            java.util.List<ChecklistResult> list = resultStore.get(res.getChecklistRun().getId());
            if (list.stream().noneMatch(existing -> existing.getId().equals(res.getId()))) {
                list.add(res);
            }
            return res;
        });
        when(checklistResultRepository.findByRunIdWithDetails(anyInt())).thenAnswer(inv ->
                resultStore.getOrDefault(inv.getArgument(0, Integer.class), List.of())
        );
        when(checklistResultRepository.findById(anyInt())).thenAnswer(inv -> {
            Integer id = inv.getArgument(0, Integer.class);
            return resultStore.values().stream()
                    .flatMap(java.util.Collection::stream)
                    .filter(r -> id.equals(r.getId()))
                    .findFirst();
        });

        // 1. Create Run #1 under V1
        Instant v1ScheduledDate = Instant.parse("2026-10-01T08:00:00Z");
        ChecklistResultRespDTO v1Init = checklistService.initVietGapAuditRun(
                10, 1, new InitVietGapAuditRunReqDTO(20, null, null, v1ScheduledDate, "V1 assessment")
        );
        assertEquals(901, v1Init.runId());
        assertEquals("V1", v1Init.checklistVersion());
        assertEquals(1, v1Init.results().size());
        assertEquals("Initial V1 Criterion", v1Init.results().get(0).ruleName());

        // Mark V1 item as pass and complete Run #1
        Integer v1ResultId = v1Init.results().get(0).resultId();
        checklistService.updateChecklistItem(
                10, 1, 901, v1ResultId,
                new UpdateChecklistItemReqDTO("pass", "Passed under V1", "https://img.com/v1.jpg", null, null)
        );
        ChecklistResultRespDTO v1Completed = checklistService.completeChecklistRun(10, 1, 901);
        assertEquals("completed", v1Completed.runStatus());
        assertEquals("V1", v1Completed.checklistVersion());
        assertNotNull(v1Completed.completedAt());
        Instant originalCompletedAt = v1Completed.completedAt();

        // 2. Admin updates criteria -> bumps checklist version to V2
        ChecklistRuleRespDTO updatedRule = ruleService.updateChecklistRule(
                300,
                new UpdateChecklistRuleReqDTO("VG-100", "Revised V2 Criterion", "Updated V2 description", "RECORDS_AND_TRAINING", "MUST", "ALL", true)
        );
        assertEquals(2, updatedRule.versionNumber());

        // 3. Re-open Run #1 -> original assessment retains results, date, snapshot, and checklistVersion V1, and stays locked
        ChecklistResultRespDTO reopenedV1 = checklistService.getChecklistResult(10, 901);
        assertEquals(901, reopenedV1.runId());
        assertEquals("V1", reopenedV1.checklistVersion());
        assertEquals(originalCompletedAt, reopenedV1.completedAt());
        assertEquals("pass", reopenedV1.results().get(0).resultStatus());
        assertEquals("Initial V1 Criterion", reopenedV1.results().get(0).ruleName());
        assertThrows(BadRequestException.class, () -> checklistService.updateChecklistItem(
                10, 1, 901, v1ResultId,
                new UpdateChecklistItemReqDTO("fail", "Attempted edit after lock", null, null, null)
        ));

        // 4. Reassess under revised V2 criteria -> creates a separate assessment record with V2
        ChecklistResultRespDTO v2Reassessment = checklistService.initVietGapAuditRun(
                10, 1, new InitVietGapAuditRunReqDTO(20, null, null, Instant.parse("2026-10-15T08:00:00Z"), "V2 reassessment")
        );
        assertNotEquals(reopenedV1.runId(), v2Reassessment.runId());
        assertEquals(902, v2Reassessment.runId());
        assertEquals("V2", v2Reassessment.checklistVersion());
        assertEquals("Revised V2 Criterion", v2Reassessment.results().get(0).ruleName());
        assertEquals(2, runStore.size());
    }
}

