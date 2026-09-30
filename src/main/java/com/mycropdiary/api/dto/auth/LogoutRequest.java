package com.mycropdiary.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

// [AI_CHANGE] Root cause: UC-05 cần DTO nhận refresh token để thu hồi khi đăng xuất
// [AI_CHANGE] Mechanism: Client gửi refresh token hiện tại, server sẽ revoke trong DB
public record LogoutRequest(
        @NotBlank(message = "Refresh token không được để trống")
        String refreshToken
) {}
