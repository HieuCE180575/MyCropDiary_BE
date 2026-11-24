package com.mycropdiary.api.dto.vietgap;

import java.math.BigDecimal;

public record VietGapCriterionRespDTO(
        Integer criterionId,
        Integer farmId,
        String farmName,
        String criterionName,
        String description,
        BigDecimal maxScore,
        String evaluationMethod,
        String standardReference,
        Boolean isActive
) {
}

