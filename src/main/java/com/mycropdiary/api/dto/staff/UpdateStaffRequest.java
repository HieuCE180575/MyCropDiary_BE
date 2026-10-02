package com.mycropdiary.api.dto.staff;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateStaffRequest(
        @Pattern(regexp = "OWNER|STAFF", message = "Vai trò không hợp lệ (OWNER hoặc STAFF)")
        String farmRole,

        @Size(max = 100, message = "Chức danh tối đa 100 ký tự")
        String jobTitle,

        @Pattern(regexp = "INVITED|ACTIVE|INACTIVE", message = "Trạng thái không hợp lệ (INVITED, ACTIVE, INACTIVE)")
        String status
) {
}
