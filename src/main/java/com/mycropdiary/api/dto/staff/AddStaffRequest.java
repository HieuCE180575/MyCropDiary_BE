package com.mycropdiary.api.dto.staff;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddStaffRequest(
        @Email(message = "Email không đúng định dạng")
        String email,

        Long userId,

        @Pattern(regexp = "OWNER|STAFF", message = "Vai trò không hợp lệ (OWNER hoặc STAFF)")
        String farmRole,

        @Size(max = 100, message = "Chức danh tối đa 100 ký tự")
        String jobTitle
) {
}
