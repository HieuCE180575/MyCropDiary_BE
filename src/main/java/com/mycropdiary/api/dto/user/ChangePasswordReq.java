package com.mycropdiary.api.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu đổi mật khẩu cho người dùng đã đăng nhập.
 */
public record ChangePasswordReq(
        @NotBlank(message = "Mật khẩu hiện tại không được để trống")
        String currentPassword,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, message = "Mật khẩu mới phải có ít nhất 8 ký tự")
        String newPassword,

        @NotBlank(message = "Vui lòng nhập lại mật khẩu mới")
        String confirmPassword
) {
}
