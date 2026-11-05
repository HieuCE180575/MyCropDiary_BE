package com.mycropdiary.api.dto.worker;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record FarmWorkerReq(
        @NotNull(message = "Thành viên trang trại (farmMemberId) không được để trống")
        Integer farmMemberId,

        @NotBlank(message = "Mã công nhân không được để trống")
        @Size(max = 40, message = "Mã công nhân tối đa 40 ký tự")
        String workerCode,

        @Size(max = 100, message = "Chức vụ / vai trò công việc tối đa 100 ký tự")
        String jobTitle,

        String employmentStatus,

        LocalDate hiredAt,

        String notes
) {
}

