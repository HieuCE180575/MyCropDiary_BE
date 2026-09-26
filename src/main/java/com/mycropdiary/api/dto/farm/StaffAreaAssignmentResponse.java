package com.mycropdiary.api.dto.farm;

import java.time.LocalDate;

public record StaffAreaAssignmentResponse(
        Long id,
        Long farmMemberId,
        Long productionAreaId,
        String areaCode,
        String areaName,
        Long assignedByMemberId,
        LocalDate startDate,
        LocalDate endDate,
        boolean active
) {}
