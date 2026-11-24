package com.mycropdiary.api.dto.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAdminUserReqDTO(
        @NotBlank(message = "Họ và tên không được để trống")
        @Size(max = 150, message = "Họ và tên không quá 150 ký tự")
        String fullName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 255, message = "Email không quá 255 ký tự")
        String email,

        @Size(max = 30, message = "Số điện thoại không quá 30 ký tự")
        String phone,

        String systemRole,

        String password
) {}
