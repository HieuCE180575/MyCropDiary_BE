package com.mycropdiary.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record GoogleAuthReq(
        @Schema(description = "ID Token credential nhận từ Google Identity Services (GSI)", example = "eyJhbGciOiJSUzI1NiIs...")
        String idToken,

        @Schema(description = "Email tài khoản Google (fallback hoặc gửi trực tiếp)", example = "user@gmail.com")
        String email,

        @Schema(description = "Họ tên tài khoản Google", example = "Hiếu Nguyễn")
        String fullName,

        @Schema(description = "Ảnh đại diện Google Profile", example = "https://lh3.googleusercontent.com/a/...")
        String avatarUrl
) {
}

