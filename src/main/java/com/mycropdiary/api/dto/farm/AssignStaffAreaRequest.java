package com.mycropdiary.api.dto.farm;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AssignStaffAreaRequest(
        @NotNull(message = "Production area ID is required")
        Long productionAreaId,

        LocalDate startDate,
        LocalDate endDate
) {}
