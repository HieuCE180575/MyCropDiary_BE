package com.mycropdiary.api.dto.farm;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO phản hồi thông tin chi tiết Vùng canh tác / Zone.
 */
public record ZoneResp(
        Integer zoneId,
        Integer farmId,
        String zoneName,
        BigDecimal areaValue,
        String areaUnit,
        String description,
        String imageUrl,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
    public ZoneResp(Integer zoneId, Integer farmId, String zoneName, BigDecimal areaValue, String areaUnit, String description, String imageUrl, Instant createdAt) {
        this(zoneId, farmId, zoneName, areaValue, areaUnit, description, imageUrl, "active", createdAt, createdAt);
    }

    public Integer id() { return zoneId; }
    public String areaName() { return zoneName; }
    public String areaCode() { return zoneName; }
    public BigDecimal areaM2() { return areaValue; }
}
