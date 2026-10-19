package com.mycropdiary.api.dto.staff;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;


public record AddStaffReq(
        @Email(message = "Email không đúng định dạng")
        String email,

        Integer userId,

        String farmRole,

        @Size(max = 100, message = "Chức danh tối đa 100 ký tự")
        String jobTitle
) {
}
