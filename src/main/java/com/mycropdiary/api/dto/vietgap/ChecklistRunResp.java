package com.mycropdiary.api.dto.vietgap;

import com.mycropdiary.api.entity.cultivation.ChecklistRun;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Kết quả lượt thực thi danh mục kiểm tra (Checklist Result & Run Details)")
public record ChecklistRunResp(
        Integer runId,
        Integer checklistRuleId,
        String ruleName,
        String ruleCategory,
        Integer farmId,
        Integer cropSeasonId,
        String seasonName,
        Integer blockId,
        String blockName,
        Integer assignedToMemberId,
        String assignedToName,
        String runStatus,
        Instant scheduledAt,
        Instant completedAt,
        Instant createdAt,
        Integer totalItems,
        Integer passedItems,
        Integer failedItems,
        Integer naItems,
        Double compliancePercent,
        Integer missingEvidenceCount,
        List<ChecklistResultItemResp> results
) {
    public static ChecklistRunResp fromEntity(ChecklistRun entity, List<ChecklistResultItemResp> itemResps) {
        if (entity == null) return null;

        Integer ruleId = entity.getChecklistRule() != null ? entity.getChecklistRule().getId() : null;
        String ruleName = entity.getChecklistRule() != null ? entity.getChecklistRule().getRuleName() : "Đợt tự đánh giá VietGAP toàn diện (33 tiêu chí)";
        String category = entity.getChecklistRule() != null ? entity.getChecklistRule().getCategory() : "VIETGAP_FULL_AUDIT";
        Integer farmId = entity.getFarm() != null ? entity.getFarm().getId() :
                (entity.getChecklistRule() != null && entity.getChecklistRule().getFarm() != null
                ? entity.getChecklistRule().getFarm().getId() :
                (entity.getCropSeason() != null && entity.getCropSeason().getFarm() != null
                ? entity.getCropSeason().getFarm().getId() : null));

        Integer seasonId = entity.getCropSeason() != null ? entity.getCropSeason().getId() : null;
        String seasonName = entity.getCropSeason() != null ? entity.getCropSeason().getSeasonName() : null;

        Integer blockId = entity.getBlock() != null ? entity.getBlock().getId() : null;
        String blockName = entity.getBlock() != null ? entity.getBlock().getBlockName() : null;

        Integer memberId = entity.getAssignedTo() != null ? entity.getAssignedTo().getId() : null;
        String memberName = entity.getAssignedTo() != null && entity.getAssignedTo().getUser() != null
                ? entity.getAssignedTo().getUser().getFullName() : null;

        int total = itemResps != null ? itemResps.size() : (entity.getTotalItems() != null ? entity.getTotalItems() : 0);
        int passed = 0;
        int failed = 0;
        int na = 0;
        int missingEvidence = 0;
        if (itemResps != null) {
            for (ChecklistResultItemResp item : itemResps) {
                if ("pass".equalsIgnoreCase(item.resultStatus())) passed++;
                else if ("fail".equalsIgnoreCase(item.resultStatus())) failed++;
                else if ("not_applicable".equalsIgnoreCase(item.resultStatus())) na++;
                if (Boolean.TRUE.equals(item.isMissingEvidence())) missingEvidence++;
            }
        }

        Double pct = entity.getCompliancePercent();
        if (pct == null && (total - na) > 0) {
            pct = Math.round(((double) passed / (total - na) * 100.0) * 10.0) / 10.0;
        }

        return new ChecklistRunResp(
                entity.getId(),
                ruleId,
                ruleName,
                category,
                farmId,
                seasonId,
                seasonName,
                blockId,
                blockName,
                memberId,
                memberName,
                entity.getRunStatus(),
                entity.getScheduledAt(),
                entity.getCompletedAt(),
                entity.getCreatedAt(),
                total,
                passed,
                failed,
                na,
                pct,
                missingEvidence,
                itemResps != null ? itemResps : List.of()
        );
    }
}

