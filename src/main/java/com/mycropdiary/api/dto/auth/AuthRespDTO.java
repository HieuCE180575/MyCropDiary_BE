package com.mycropdiary.api.dto.auth;


public record AuthRespDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        Integer userId,
        String email,
        String fullName,
        String systemRole) {

    public static AuthRespDTO of(String accessToken, String refreshToken,
                                  Integer userId, String email, String fullName, String systemRole) {
        return new AuthRespDTO(accessToken, refreshToken, "Bearer", userId, email, fullName, systemRole);
    }
}
