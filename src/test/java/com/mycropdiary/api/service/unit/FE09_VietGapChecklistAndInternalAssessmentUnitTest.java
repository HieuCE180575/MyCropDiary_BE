package com.mycropdiary.api.service.unit;

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
import com.mycropdiary.api.service.impl.InternalAssessmentServiceImpl;
import com.mycropdiary.api.service.impl.VietGapChecklistServiceImpl;
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
    private ChecklistRunRepository checklistRunRepository;
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
    private VietGapChecklistServiceImpl checklistService;

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

        RunChecklistReq req = new RunChecklistReq(
                100, null, null, null, null,
                "Check before harvesting tomato crop",
                List.of(new SubmitChecklistItemReq("pass", "14 days quarantine satisfied", "https://img.com/evidence1.jpg"))
        );

        ChecklistRunResp resp = checklistService.runChecklist(10, 1, req);
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

        RunChecklistReq req = new RunChecklistReq(
                100, null, null, null, null, "Check",
                List.of(new SubmitChecklistItemReq("UNKNOWN_STATUS", "Note", null))
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

        List<ChecklistResultItemResp> items = checklistService.getChecklistItemDetails(10, 501);
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

        ConductInternalAssessmentReq req = new ConductInternalAssessmentReq(
                "Quarterly Internal Audit Q1", null, null, Instant.now(), "Overall good adherence", null,
                List.of(
                        new InternalAssessmentItemReq(1, new BigDecimal("90.00"), "Safe distance to industry", "https://pic.com/1.jpg"),
                        new InternalAssessmentItemReq(2, new BigDecimal("85.00"), "Organic compost certified", "https://pic.com/2.jpg")
                )
        );

        InternalAssessmentResp resp = assessmentService.conductInternalAssessment(10, 1, req);
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

        ConductInternalAssessmentReq req = new ConductInternalAssessmentReq(
                "Audit", null, null, Instant.now(), null, null,
                List.of(new InternalAssessmentItemReq(1, new BigDecimal("150.00"), "Over max score", null))
        );

        assertThrows(BadRequestException.class, () -> assessmentService.conductInternalAssessment(10, 1, req));
    }
}

