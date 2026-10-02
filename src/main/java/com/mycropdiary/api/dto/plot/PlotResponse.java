package com.mycropdiary.api.dto.plot;

import java.math.BigDecimal;

public record PlotResponse(
        Long id,
        Long productionAreaId,
        String productionAreaCode,
        String productionAreaName,
        Long farmId,
        String plotCode,
        String plotName,
        BigDecimal areaM2,
        BigDecimal latitude,
        BigDecimal longitude,
        String boundaryGeoJson,
        String status
) {
}
