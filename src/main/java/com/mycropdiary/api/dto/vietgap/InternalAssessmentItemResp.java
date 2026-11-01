package com.mycropdiary.api.dto.vietgap;

import com.mycropdiary.api.entity.cultivation.InternalAssessmentItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Chi tiết điểm số và bằng chứng của từng tiêu chí trong đợt đánh giá nội bộ")
public record InternalAssessmentItemResp(
        Integer itemId,
        Integer criterionId,
        String criterionName,
        String criterionDescription,
        BigDecimal maxScore,
        BigDecimal score,
        String resultNotes,
        String evidenceImageUrl,
        Boolean isEvidenceAttached
) {
    public static InternalAssessmentItemResp fromEntity(InternalAssessmentItem entity) {
        if (entity == null) return null;
        Integer criterionId = entity.getAssessmentCriterion() != null ? entity.getAssessmentCriterion().getId() : null;
        String criterionName = entity.getAssessmentCriterion() != null ? entity.getAssessmentCriterion().getCriterionName() : null;
        String description = entity.getAssessmentCriterion() != null ? entity.getAssessmentCriterion().getDescription() : null;
        BigDecimal maxScore = entity.getAssessmentCriterion() != null ? entity.getAssessmentCriterion().getMaxScore() : new BigDecimal("100.00");

        boolean hasEvidence = entity.getEvidenceImageUrl() != null && !entity.getEvidenceImageUrl().isBlank();

        return new InternalAssessmentItemResp(
                entity.getId(),
                criterionId,
                criterionName,
                description,
                maxScore,
                entity.getScore(),
                entity.getResultNotes(),
                entity.getEvidenceImageUrl(),
                hasEvidence
        );
    }
}

