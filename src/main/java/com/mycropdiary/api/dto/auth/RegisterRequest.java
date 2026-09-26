package com.mycropdiary.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// [AI_CHANGE] Root cause: Bổ sung validation messages tiếng Việt rõ ràng cho UX người dùng
public record RegisterRequest(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
        String fullName,

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Size(max = 254, message = "Email tối đa 254 ký tự")
        String email,

        @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
        String phoneNumber,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 đến 100 ký tự")
        String password) {
}
