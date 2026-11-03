package com.mycropdiary.api.dto.water;

import com.mycropdiary.api.entity.WaterAssessment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Thông tin kết quả đánh giá chất lượng nguồn nước tưới tiêu (Water Assessment Details)")
public record WaterAssessmentResp(
        Integer assessmentId,
        Integer waterSourceId,
        String waterSourceName,
        String waterSourceType,
        Integer blockId,
        String blockName,
        Integer zoneId,
        String zoneName,
        Integer farmId,
        Integer conductedByMemberId,
        String conductedByName,
        BigDecimal waterPh,
        BigDecimal temperatureC,
        String qualityStatus,
        String notes,
        String imageUrl,
        Instant assessedAt
) {
    public static WaterAssessmentResp fromEntity(WaterAssessment entity) {
        if (entity == null) return null;

        Integer memberId = entity.getConductedBy() != null ? entity.getConductedBy().getId() : null;
        String memberName = entity.getConductedBy() != null && entity.getConductedBy().getUser() != null
                ? entity.getConductedBy().getUser().getFullName() : null;

        Integer blockId = entity.getBlock() != null ? entity.getBlock().getId() : null;
        String blockName = entity.getBlock() != null ? entity.getBlock().getBlockName() : null;
        Integer zoneId = entity.getBlock() != null && entity.getBlock().getZone() != null
                ? entity.getBlock().getZone().getId() : null;
        String zoneName = entity.getBlock() != null && entity.getBlock().getZone() != null
                ? entity.getBlock().getZone().getZoneName() : null;

        Integer farmId = entity.getWaterSource() != null && entity.getWaterSource().getFarm() != null
                ? entity.getWaterSource().getFarm().getId()
                : (entity.getBlock() != null && entity.getBlock().getZone() != null && entity.getBlock().getZone().getFarm() != null
                ? entity.getBlock().getZone().getFarm().getId() : null);

        return new WaterAssessmentResp(
                entity.getId(),
                entity.getWaterSource() != null ? entity.getWaterSource().getId() : null,
                entity.getWaterSource() != null ? entity.getWaterSource().getSourceName() : null,
                entity.getWaterSource() != null ? entity.getWaterSource().getSourceType() : null,
                blockId,
                blockName,
                zoneId,
                zoneName,
                farmId,
                memberId,
                memberName,
                entity.getWaterPh(),
                entity.getTemperatureC(),
                entity.getQualityStatus(),
                entity.getNotes(),
                entity.getImageUrl(),
                entity.getAssessedAt()
        );
    }
}

