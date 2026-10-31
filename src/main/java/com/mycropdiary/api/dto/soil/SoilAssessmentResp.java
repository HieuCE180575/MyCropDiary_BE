package com.mycropdiary.api.dto.soil;

import com.mycropdiary.api.entity.SoilAssessment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Thông tin kết quả đánh giá thổ nhưỡng (Soil Assessment Details)")
public record SoilAssessmentResp(
        Integer assessmentId,
        Integer blockId,
        String blockName,
        Integer zoneId,
        String zoneName,
        Integer farmId,
        Integer conductedByMemberId,
        String conductedByName,
        BigDecimal soilPh,
        BigDecimal moisturePercent,
        String nutrientData,
        String notes,
        String imageUrl,
        Instant assessedAt
) {
    public static SoilAssessmentResp fromEntity(SoilAssessment entity) {
        if (entity == null) return null;
        Integer memberId = entity.getConductedBy() != null ? entity.getConductedBy().getId() : null;
        String memberName = entity.getConductedBy() != null && entity.getConductedBy().getUser() != null
                ? entity.getConductedBy().getUser().getFullName() : null;

        return new SoilAssessmentResp(
                entity.getId(),
                entity.getBlock().getId(),
                entity.getBlock().getBlockName(),
                entity.getBlock().getZone().getId(),
                entity.getBlock().getZone().getZoneName(),
                entity.getBlock().getZone().getFarm().getId(),
                memberId,
                memberName,
                entity.getSoilPh(),
                entity.getMoisturePercent(),
                entity.getNutrientData(),
                entity.getNotes(),
                entity.getImageUrl(),
                entity.getAssessedAt()
        );
    }
}

