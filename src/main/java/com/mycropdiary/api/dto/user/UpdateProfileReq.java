package com.mycropdiary.api.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu cập nhật hồ sơ người dùng.
 */
public record UpdateProfileReq(
        @NotBlank(message = "Họ và tên không được để trống")
        @Size(max = 150, message = "Họ và tên không được vượt quá 150 ký tự")
        String fullName,

        @Pattern(regexp = "^(0[35789]\\d{8})?$", message = "Số điện thoại không hợp lệ (định dạng 10 số Việt Nam)")
        String phone,

        @Size(max = 1000, message = "URL ảnh đại diện không vượt quá 1000 ký tự")
        String avatarUrl
) {
}
