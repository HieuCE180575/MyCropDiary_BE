package com.mycropdiary.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// [AI_CHANGE] Root cause: UC-06 cần DTO nhận email + OTP + mật khẩu mới để đặt lại mật khẩu
// [AI_CHANGE] Mechanism: Validate email, OTP không rỗng, mật khẩu mới tối thiểu 8 ký tự
public record ResetPasswordRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @NotBlank(message = "Mã OTP không được để trống")
        String otp,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, max = 100, message = "Mật khẩu mới phải từ 8 đến 100 ký tự")
        String newPassword
) {}
