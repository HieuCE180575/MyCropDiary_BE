package com.mycropdiary.api.dto.risk;

import com.mycropdiary.api.entity.ZoneRiskAssessment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Thông tin chi tiết rủi ro phân khu (Zone Risk Details)")
public record ZoneRiskResp(
        Integer riskId,
        Integer zoneId,
        String zoneName,
        Integer farmId,
        Integer assessedByMemberId,
        String assessedByName,
        String riskType,
        String riskLevel,
        String description,
        String mitigationPlan,
        String imageUrl,
        Instant assessedAt
) {
    public static ZoneRiskResp fromEntity(ZoneRiskAssessment entity) {
        if (entity == null) return null;
        Integer memberId = entity.getAssessedBy() != null ? entity.getAssessedBy().getId() : null;
        String memberName = entity.getAssessedBy() != null && entity.getAssessedBy().getUser() != null
                ? entity.getAssessedBy().getUser().getFullName() : null;

        return new ZoneRiskResp(
                entity.getId(),
                entity.getZone().getId(),
                entity.getZone().getZoneName(),
                entity.getZone().getFarm().getId(),
                memberId,
                memberName,
                entity.getRiskType(),
                entity.getRiskLevel(),
                entity.getDescription(),
                entity.getMitigationPlan(),
                entity.getImageUrl(),
                entity.getAssessedAt()
        );
    }
}

