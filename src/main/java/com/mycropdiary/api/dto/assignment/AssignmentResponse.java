package com.mycropdiary.api.dto.assignment;

import java.time.LocalDate;

public record AssignmentResponse(
        Long id,
        Long farmMemberId,
        Long userId,
        String staffName,
        String staffEmail,
        String staffJobTitle,
        Long productionAreaId,
        String areaCode,
        String areaName,
        Long assignedByMemberId,
        String assignerName,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isActive
) {
}
