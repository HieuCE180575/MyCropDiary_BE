package com.mycropdiary.api.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request cập nhật thông tin tài khoản người dùng bởi Admin")
public record UpdateUserReqDTO(
        @Schema(description = "Họ và tên người dùng", example = "Nguyễn Văn An")
        @Size(max = 150, message = "Họ và tên không được vượt quá 150 ký tự")
        String fullName,

        @Schema(description = "Số điện thoại liên hệ", example = "0912345678")
        @Pattern(regexp = "^([+0-9 .\\-()]{8,20})?$", message = "Số điện thoại không hợp lệ")
        String phone,

        @Schema(description = "Vai trò hệ thống (USER, OWNER, STAFF, ADMIN)", example = "OWNER")
        String systemRole,

        @Schema(description = "Trạng thái tài khoản (ACTIVE, LOCKED, PENDING)", example = "ACTIVE")
        String accountStatus
) {
}
