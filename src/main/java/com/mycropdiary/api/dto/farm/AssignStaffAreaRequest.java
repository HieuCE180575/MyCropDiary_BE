package com.mycropdiary.api.dto.farm;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * DTO yêu cầu phân công nhân viên quản lý Vùng sản xuất.
 */
public record AssignStaffAreaRequest(
        @NotNull(message = "Production area ID is required")
        Long productionAreaId,

        LocalDate startDate,
        LocalDate endDate
) {}
