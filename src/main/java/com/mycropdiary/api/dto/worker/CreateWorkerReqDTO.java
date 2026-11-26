package com.mycropdiary.api.dto.worker;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateWorkerReqDTO(
        Integer farmMemberId,

        @Size(max = 120, message = "Họ tên công nhân tối đa 120 ký tự")
        String fullName,

        @Size(max = 30, message = "Số điện thoại tối đa 30 ký tự")
        String phoneNumber,

        @NotBlank(message = "Mã công nhân không được để trống")
        @Size(max = 40, message = "Mã công nhân tối đa 40 ký tự")
        String workerCode,

        @Size(max = 100, message = "Chức vụ / vai trò công việc tối đa 100 ký tự")
        String jobTitle,

        String employmentStatus,

        LocalDate hiredAt,

        String notes
) {
    public CreateWorkerReqDTO(Integer farmMemberId, String workerCode, String jobTitle, String employmentStatus, LocalDate hiredAt, String notes) {
        this(farmMemberId, null, null, workerCode, jobTitle, employmentStatus, hiredAt, notes);
    }
}

