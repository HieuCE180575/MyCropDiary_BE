package com.mycropdiary.api.dto.cultivation;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CropSeasonBlockResp(
        Integer cropSeasonId,
        String seasonName,
        Integer blockId,
        String blockName,
        Integer zoneId,
        String zoneName,
        BigDecimal blockArea,
        String blockAreaUnit,
        String soilType,
        BigDecimal plantedArea,
        String areaUnit,
        LocalDate plantedAt,
        LocalDate expectedHarvestAt
) {
}
