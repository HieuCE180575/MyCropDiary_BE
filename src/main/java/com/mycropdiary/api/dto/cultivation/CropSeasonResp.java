package com.mycropdiary.api.dto.cultivation;

import java.time.Instant;
import java.time.LocalDate;

public record CropSeasonResp(
        Integer cropSeasonId,
        Integer farmId,
        String farmName,
        Integer cropCategoryId,
        String cropCategoryName,
        String seasonName,
        LocalDate startDate,
        LocalDate endDate,
        String seasonStatus,
        String notes,
        Integer createdByUserId,
        String createdByFullName,
        Instant createdAt
) {
}

