package com.mycropdiary.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// [AI_CHANGE] Root cause: UC-06 cần DTO nhận email để gửi OTP đặt lại mật khẩu
// [AI_CHANGE] Mechanism: Validate email hợp lệ và không rỗng
public record ForgotPasswordRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email
) {}
