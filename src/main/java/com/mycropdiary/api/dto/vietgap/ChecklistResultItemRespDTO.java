package com.mycropdiary.api.dto.vietgap;

import com.mycropdiary.api.entity.cultivation.ChecklistResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Chi tiết kết quả mục kiểm tra và bằng chứng hiện trường (Checklist Item Details and Missing Evidence)")
public record ChecklistResultItemRespDTO(
        Integer resultId,
        Integer runId,
        Integer checklistRuleId,
        String ruleCode,
        String ruleName,
        String category,
        String ruleLevel,
        Integer checkedByMemberId,
        String checkedByName,
        String resultStatus, // 'pass', 'fail', 'not_applicable', 'pending'
        String notes,
        String evidenceImageUrl,
        String evidenceReferenceType,
        Integer evidenceReferenceId,
        Boolean isAutoEvaluated,
        Boolean isMissingEvidence,
        Instant checkedAt
) {
    public static ChecklistResultItemRespDTO fromEntity(ChecklistResult entity) {
        if (entity == null) return null;
        Integer memberId = entity.getCheckedBy() != null ? entity.getCheckedBy().getId() : null;
        String memberName = entity.getCheckedBy() != null && entity.getCheckedBy().getUser() != null
                ? entity.getCheckedBy().getUser().getFullName() : null;

        Integer ruleId = entity.getChecklistRule() != null ? entity.getChecklistRule().getId() : null;
        String code = entity.getRuleCode() != null ? entity.getRuleCode() :
                (entity.getChecklistRule() != null ? entity.getChecklistRule().getRuleCode() : null);
        String name = entity.getRuleName() != null ? entity.getRuleName() :
                (entity.getChecklistRule() != null ? entity.getChecklistRule().getRuleName() : null);
        String cat = entity.getCategory() != null ? entity.getCategory() :
                (entity.getChecklistRule() != null ? entity.getChecklistRule().getCategory() : null);
        String level = entity.getRuleLevel() != null ? entity.getRuleLevel() :
                (entity.getChecklistRule() != null ? entity.getChecklistRule().getRuleLevel() : "MUST");

        boolean missingEvidence = "fail".equalsIgnoreCase(entity.getResultStatus())
                || (("pass".equalsIgnoreCase(entity.getResultStatus()) || "MUST".equalsIgnoreCase(level))
                    && (entity.getEvidenceImageUrl() == null || entity.getEvidenceImageUrl().isBlank())
                    && entity.getEvidenceReferenceId() == null);

        return new ChecklistResultItemRespDTO(
                entity.getId(),
                entity.getChecklistRun() != null ? entity.getChecklistRun().getId() : null,
                ruleId,
                code,
                name,
                cat,
                level,
                memberId,
                memberName,
                entity.getResultStatus(),
                entity.getNotes(),
                entity.getEvidenceImageUrl(),
                entity.getEvidenceReferenceType(),
                entity.getEvidenceReferenceId(),
                entity.getIsAutoEvaluated(),
                missingEvidence,
                entity.getCheckedAt()
        );
    }
}

