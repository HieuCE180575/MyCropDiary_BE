package com.mycropdiary.api.dto.vietgap;

import com.mycropdiary.api.entity.cultivation.ChecklistResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Chi tiết kết quả mục kiểm tra và bằng chứng hiện trường (Checklist Item Details and Missing Evidence)")
public record ChecklistResultItemResp(
        Integer resultId,
        Integer runId,
        Integer checkedByMemberId,
        String checkedByName,
        String resultStatus, // 'pass', 'fail', 'not_applicable'
        String notes,
        String evidenceImageUrl,
        Boolean isMissingEvidence,
        Instant checkedAt
) {
    public static ChecklistResultItemResp fromEntity(ChecklistResult entity) {
        if (entity == null) return null;
        Integer memberId = entity.getCheckedBy() != null ? entity.getCheckedBy().getId() : null;
        String memberName = entity.getCheckedBy() != null && entity.getCheckedBy().getUser() != null
                ? entity.getCheckedBy().getUser().getFullName() : null;

        boolean missingEvidence = "fail".equalsIgnoreCase(entity.getResultStatus())
                || entity.getEvidenceImageUrl() == null
                || entity.getEvidenceImageUrl().isBlank();

        return new ChecklistResultItemResp(
                entity.getId(),
                entity.getChecklistRun().getId(),
                memberId,
                memberName,
                entity.getResultStatus(),
                entity.getNotes(),
                entity.getEvidenceImageUrl(),
                missingEvidence,
                entity.getCheckedAt()
        );
    }
}

