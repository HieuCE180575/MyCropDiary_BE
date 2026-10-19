package com.mycropdiary.api.dto.auth;


public record AuthResp(
        String accessToken,
        String refreshToken,
        String tokenType,
        Integer userId,
        String email,
        String fullName,
        String systemRole) {

    public static AuthResp of(String accessToken, String refreshToken,
                                  Integer userId, String email, String fullName, String systemRole) {
        return new AuthResp(accessToken, refreshToken, "Bearer", userId, email, fullName, systemRole);
    }
}
