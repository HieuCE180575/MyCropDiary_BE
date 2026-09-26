package com.mycropdiary.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// [AI_CHANGE] Root cause: Cần DTO cho UC-04 xác thực OTP sau khi đăng ký
public record VerifyOtpRequest(
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        String email,

        @NotBlank(message = "Mã OTP không được để trống")
        @Size(min = 6, max = 6, message = "Mã OTP phải gồm 6 chữ số")
        String otp) {
}
