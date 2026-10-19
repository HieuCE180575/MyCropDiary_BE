package com.mycropdiary.api.dto.auth;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        UUID userId,
        String email,
        String fullName,
        String systemRole) {

    public static AuthResponse of(String accessToken, String refreshToken,
                                  UUID userId, String email, String fullName, String systemRole) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", userId, email, fullName, systemRole);
    }
}
