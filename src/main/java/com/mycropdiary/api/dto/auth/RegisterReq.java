package com.mycropdiary.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// [AI_CHANGE] Root cause: Bổ sung validation messages tiếng Việt rõ ràng cho UX người dùng
// [AI_CHANGE] Enforce password criteria: 8-100 ký tự, ít nhất 1 chữ hoa, 1 chữ thường, 1 ký tự đặc biệt
public record RegisterReq(
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
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,100}$",
                message = "Mật khẩu phải từ 8 đến 100 ký tự, chứa ít nhất 1 chữ in hoa, 1 chữ in thường và 1 ký tự đặc biệt"
        )
        String password) {
}
