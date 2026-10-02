package com.mycropdiary.api.dto.assignment;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssignedAreaResponse(
        Long productionAreaId,
        Long farmId,
        String areaCode,
        String areaName,
        BigDecimal areaM2,
        String description,
        String status,
        Long assignmentId,
        LocalDate assignedStartDate,
        int activePlotCount
) {
}
