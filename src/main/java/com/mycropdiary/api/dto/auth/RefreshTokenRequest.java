package com.mycropdiary.api.dto.auth;

import jakarta.validation.constraints.NotBlank;

// [AI_CHANGE] Root cause: UC-05 cần DTO nhận refresh token để cấp access token mới
// [AI_CHANGE] Mechanism: Client gửi refresh token còn hạn, server validate và cấp access token mới
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token không được để trống")
        String refreshToken
) {}
