package com.mycropdiary.api.dto.cultivation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LotTraceabilityRespDTO(
        Integer harvestRecordId,
        String harvestLotCode,
        String traceabilityCode,
        Integer farmId,
        String farmName,
        Integer cropSeasonId,
        String seasonName,
        Integer blockId,
        String blockName,
        LocalDateTime harvestedAt,
        BigDecimal quantity,
        String unit,
        String qualityGrade,
        String notes
) {
}
