package com.mycropdiary.api.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu đổi mật khẩu cho người dùng đã đăng nhập.
 * Quy tắc mật khẩu đồng nhất với Đăng ký & Quên mật khẩu:
 * 8-100 ký tự, chứa ít nhất 1 chữ in hoa, 1 chữ in thường và 1 ký tự đặc biệt.
 */
public record ChangePasswordReqDTO(
        @NotBlank(message = "Mật khẩu hiện tại không được để trống")
        String currentPassword,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, max = 100, message = "Mật khẩu mới phải từ 8 đến 100 ký tự")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,100}$",
                message = "Mật khẩu mới phải từ 8 đến 100 ký tự, chứa ít nhất 1 chữ in hoa, 1 chữ in thường và 1 ký tự đặc biệt"
        )
        String newPassword,

        @NotBlank(message = "Vui lòng nhập lại mật khẩu mới")
        String confirmPassword
) {
}
