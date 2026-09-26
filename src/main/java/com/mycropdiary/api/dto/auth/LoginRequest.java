package com.mycropdiary.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// [AI_CHANGE] Root cause: Cần DTO cho UC-03 API đăng nhập với email + password
public record LoginRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 254, message = "Email tối đa 254 ký tự")
        String email,

        @NotBlank(message = "Mật khẩu không được để trống")
        String password) {
}
