package com.mycropdiary.api.dto.assignment;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateAssignmentRequest(
        @NotNull(message = "ID nhân viên trang trại (farmMemberId) không được để trống")
        Long farmMemberId,

        @NotNull(message = "ID khu vực sản xuất (productionAreaId) không được để trống")
        Long productionAreaId,

        LocalDate startDate,

        LocalDate endDate
) {
}
