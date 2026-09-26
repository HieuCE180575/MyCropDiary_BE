package com.mycropdiary.api.dto.farm;

import java.time.LocalDate;

/**
 * DTO phản hồi thông tin phân công nhân viên quản lý Vùng sản xuất.
 */
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
