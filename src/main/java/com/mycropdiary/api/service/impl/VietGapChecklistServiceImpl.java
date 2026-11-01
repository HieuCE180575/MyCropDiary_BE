package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.vietgap.ChecklistResultItemResp;
import com.mycropdiary.api.dto.vietgap.ChecklistRunResp;
import com.mycropdiary.api.dto.vietgap.RunChecklistReq;
import com.mycropdiary.api.dto.vietgap.SubmitChecklistItemReq;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.cultivation.ChecklistResult;
import com.mycropdiary.api.entity.cultivation.ChecklistRule;
import com.mycropdiary.api.entity.cultivation.ChecklistRun;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.cultivation.ChecklistResultRepository;
import com.mycropdiary.api.repository.cultivation.ChecklistRuleRepository;
import com.mycropdiary.api.repository.cultivation.ChecklistRunRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.service.cultivation.VietGapChecklistService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class VietGapChecklistServiceImpl implements VietGapChecklistService {

    private static final Set<String> VALID_RESULT_STATUSES = Set.of("pass", "fail", "not_applicable");
    private static final Set<String> VALID_RUN_STATUSES = Set.of("open", "in_progress", "completed", "cancelled");

    private final ChecklistRuleRepository checklistRuleRepository;
    private final ChecklistRunRepository checklistRunRepository;
    private final ChecklistResultRepository checklistResultRepository;
    private final CropSeasonRepository cropSeasonRepository;
    private final BlockRepository blockRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final FarmAccessGuard accessGuard;

    public VietGapChecklistServiceImpl(ChecklistRuleRepository checklistRuleRepository,
                                       ChecklistRunRepository checklistRunRepository,
                                       ChecklistResultRepository checklistResultRepository,
                                       CropSeasonRepository cropSeasonRepository,
                                       BlockRepository blockRepository,
                                       FarmMemberRepository farmMemberRepository,
                                       FarmAccessGuard accessGuard) {
        this.checklistRuleRepository = checklistRuleRepository;
        this.checklistRunRepository = checklistRunRepository;
        this.checklistResultRepository = checklistResultRepository;
        this.cropSeasonRepository = cropSeasonRepository;
        this.blockRepository = blockRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    public ChecklistRunResp runChecklist(Integer currentUserId, Integer farmId, RunChecklistReq request) {
        accessGuard.requireFarmAccess(currentUserId, farmId);

        ChecklistRule rule = checklistRuleRepository.findById(request.checklistRuleId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quy tắc kiểm tra ID: " + request.checklistRuleId()));

        if (!rule.getFarm().getId().equals(farmId)) {
            throw new BadRequestException("Quy tắc kiểm tra không thuộc trang trại này");
        }

        CropSeason season = null;
        if (request.cropSeasonId() != null) {
            season = cropSeasonRepository.findById(request.cropSeasonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mùa vụ ID: " + request.cropSeasonId()));
            if (!season.getFarm().getId().equals(farmId)) {
                throw new BadRequestException("Mùa vụ không thuộc trang trại này");
            }
        }

        Block block = null;
        if (request.blockId() != null) {
            block = blockRepository.findById(request.blockId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô đất ID: " + request.blockId()));
            if (!block.getZone().getFarm().getId().equals(farmId)) {
                throw new BadRequestException("Lô đất không thuộc trang trại này");
            }
        }

        FarmMember assignedMember = null;
        if (request.assignedToMemberId() != null) {
            assignedMember = farmMemberRepository.findById(request.assignedToMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thành viên phụ trách ID: " + request.assignedToMemberId()));
            if (!assignedMember.getFarm().getId().equals(farmId)) {
                throw new BadRequestException("Thành viên phụ trách không thuộc trang trại này");
            }
        } else {
            assignedMember = farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, currentUserId, "active").orElse(null);
        }

        boolean hasItems = request.items() != null && !request.items().isEmpty();
        String initialStatus = hasItems ? "completed" : "open";
        Instant completedAt = hasItems ? Instant.now() : null;

        ChecklistRun run = new ChecklistRun(
                rule,
                season,
                block,
                assignedMember,
                initialStatus,
                request.scheduledAt() != null ? request.scheduledAt() : Instant.now(),
                completedAt
        );

        ChecklistRun savedRun = checklistRunRepository.save(run);

        List<ChecklistResultItemResp> resultResps = new ArrayList<>();
        if (hasItems) {
            for (SubmitChecklistItemReq itemReq : request.items()) {
                String status = itemReq.resultStatus().toLowerCase().trim();
                if (!VALID_RESULT_STATUSES.contains(status)) {
                    throw new BadRequestException("Trạng thái kiểm tra không hợp lệ: " + itemReq.resultStatus()
                            + ". Chỉ chấp nhận: pass, fail, not_applicable.");
                }

                ChecklistResult result = new ChecklistResult(
                        savedRun,
                        assignedMember,
                        status,
                        itemReq.notes() != null ? itemReq.notes().trim() : null,
                        itemReq.evidenceImageUrl() != null ? itemReq.evidenceImageUrl().trim() : null,
                        Instant.now()
                );
                ChecklistResult savedResult = checklistResultRepository.save(result);
                resultResps.add(ChecklistResultItemResp.fromEntity(savedResult));
            }
        }

        return ChecklistRunResp.fromEntity(savedRun, resultResps);
    }

    @Override
    @Transactional(readOnly = true)
    public ChecklistRunResp getChecklistResult(Integer currentUserId, Integer runId) {
        ChecklistRun run = checklistRunRepository.findByIdWithDetails(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt chạy kiểm tra ID: " + runId));

        accessGuard.requireFarmAccess(currentUserId, run.getChecklistRule().getFarm().getId());

        List<ChecklistResult> results = checklistResultRepository.findByRunIdWithDetails(runId);
        List<ChecklistResultItemResp> itemResps = results.stream().map(ChecklistResultItemResp::fromEntity).toList();

        return ChecklistRunResp.fromEntity(run, itemResps);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistRunResp> getChecklistRunHistory(Integer currentUserId, Integer farmId, Integer cropSeasonId, String runStatus) {
        accessGuard.requireFarmAccess(currentUserId, farmId);

        String trimmedStatus = (runStatus != null && !runStatus.isBlank()) ? runStatus.toLowerCase().trim() : null;
        if (trimmedStatus != null && !VALID_RUN_STATUSES.contains(trimmedStatus)) {
            throw new BadRequestException("Trạng thái lượt kiểm tra không hợp lệ: " + runStatus
                    + ". Chỉ chấp nhận: open, in_progress, completed, cancelled.");
        }

        List<ChecklistRun> runs = checklistRunRepository.findRunsByFarm(farmId, cropSeasonId, trimmedStatus);

        return runs.stream().map(run -> {
            List<ChecklistResult> results = checklistResultRepository.findByRunIdWithDetails(run.getId());
            List<ChecklistResultItemResp> itemResps = results.stream().map(ChecklistResultItemResp::fromEntity).toList();
            return ChecklistRunResp.fromEntity(run, itemResps);
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistResultItemResp> getChecklistItemDetails(Integer currentUserId, Integer runId) {
        ChecklistRun run = checklistRunRepository.findByIdWithDetails(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lượt chạy kiểm tra ID: " + runId));

        accessGuard.requireFarmAccess(currentUserId, run.getChecklistRule().getFarm().getId());

        List<ChecklistResult> results = checklistResultRepository.findByRunIdWithDetails(runId);
        return results.stream().map(ChecklistResultItemResp::fromEntity).toList();
    }
}

