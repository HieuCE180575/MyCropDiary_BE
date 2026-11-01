package com.mycropdiary.api.dto.vietgap;

import com.mycropdiary.api.entity.cultivation.InternalAssessment;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Schema(description = "Kết quả và hồ sơ chi tiết đợt đánh giá nội bộ VietGAP (Internal Assessment Details)")
public record InternalAssessmentResp(
        Integer internalAssessmentId,
        Integer farmId,
        Integer cropSeasonId,
        String seasonName,
        Integer zoneId,
        String zoneName,
        Integer assessedByMemberId,
        String assessedByName,
        String assessmentTitle,
        Instant assessedAt,
        String notes,
        String evidenceImageUrl,
        BigDecimal totalScore,
        BigDecimal maxPossibleScore,
        BigDecimal scorePercentage,
        String complianceStatus, // 'COMPLIANT' (>=80%), 'CONDITIONALLY_COMPLIANT' (>=65%), 'NON_COMPLIANT' (<65%)
        Integer totalCriteria,
        Integer criteriaWithEvidence,
        List<InternalAssessmentItemResp> items
) {
    public static InternalAssessmentResp fromEntity(InternalAssessment entity, List<InternalAssessmentItemResp> itemResps) {
        if (entity == null) return null;

        Integer farmId = entity.getFarm() != null ? entity.getFarm().getId() : null;
        Integer seasonId = entity.getCropSeason() != null ? entity.getCropSeason().getId() : null;
        String seasonName = entity.getCropSeason() != null ? entity.getCropSeason().getSeasonName() : null;

        Integer zoneId = entity.getZone() != null ? entity.getZone().getId() : null;
        String zoneName = entity.getZone() != null ? entity.getZone().getZoneName() : null;

        Integer memberId = entity.getAssessedBy() != null ? entity.getAssessedBy().getId() : null;
        String memberName = entity.getAssessedBy() != null && entity.getAssessedBy().getUser() != null
                ? entity.getAssessedBy().getUser().getFullName() : null;

        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxScore = BigDecimal.ZERO;
        int withEvidence = 0;

        if (itemResps != null) {
            for (InternalAssessmentItemResp item : itemResps) {
                if (item.score() != null) totalScore = totalScore.add(item.score());
                if (item.maxScore() != null) maxScore = maxScore.add(item.maxScore());
                if (Boolean.TRUE.equals(item.isEvidenceAttached())) withEvidence++;
            }
        }

        BigDecimal percentage = BigDecimal.ZERO;
        if (maxScore.compareTo(BigDecimal.ZERO) > 0) {
            percentage = totalScore.multiply(BigDecimal.valueOf(100)).divide(maxScore, 2, RoundingMode.HALF_UP);
        }

        String compliance;
        if (percentage.compareTo(BigDecimal.valueOf(80.0)) >= 0) {
            compliance = "COMPLIANT";
        } else if (percentage.compareTo(BigDecimal.valueOf(65.0)) >= 0) {
            compliance = "CONDITIONALLY_COMPLIANT";
        } else {
            compliance = "NON_COMPLIANT";
        }

        return new InternalAssessmentResp(
                entity.getId(),
                farmId,
                seasonId,
                seasonName,
                zoneId,
                zoneName,
                memberId,
                memberName,
                entity.getAssessmentTitle(),
                entity.getAssessedAt(),
                entity.getNotes(),
                entity.getEvidenceImageUrl(),
                totalScore,
                maxScore,
                percentage,
                compliance,
                itemResps != null ? itemResps.size() : 0,
                withEvidence,
                itemResps != null ? itemResps : List.of()
        );
    }
}

