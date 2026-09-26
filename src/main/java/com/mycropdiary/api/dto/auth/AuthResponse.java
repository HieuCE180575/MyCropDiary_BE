package com.mycropdiary.api.dto.auth;

// [AI_CHANGE] Root cause: Response chuẩn hóa cho API đăng nhập và xác thực OTP thành công
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long userId,
        String email,
        String fullName,
        String systemRole) {

    public static AuthResponse of(String accessToken, String refreshToken,
                                  Long userId, String email, String fullName, String systemRole) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", userId, email, fullName, systemRole);
    }
}
