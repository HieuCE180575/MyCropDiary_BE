package com.mycropdiary.api.dto.block;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO phản hồi thông tin chi tiết lô / thửa đất (Block).
 */
public record BlockResp(
        Integer blockId,
        Integer zoneId,
        String zoneName,
        Integer farmId,
        String blockName,
        BigDecimal areaValue,
        String areaUnit,
        String soilType,
        String description,
        String imageUrl,
        String status,
        BigDecimal latitude,
        BigDecimal longitude,
        String boundaryGeoJson,
        Instant createdAt
) {
    public Integer id() { return blockId; }
    public Integer productionAreaId() { return zoneId; }
    public String productionAreaName() { return zoneName; }
    public String productionAreaCode() { return zoneName; }
    public String plotName() { return blockName; }
    public String plotCode() { return blockName; }
    public String blockCode() { return blockName; }
    public BigDecimal areaM2() { return areaValue; }
}
