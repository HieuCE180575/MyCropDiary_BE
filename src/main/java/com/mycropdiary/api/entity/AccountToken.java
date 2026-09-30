package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

// [AI_CHANGE] Root cause: Cần bảng AccountToken quản lý OTP xác thực email và Refresh Token
// [AI_CHANGE] Mechanism: Ánh xạ chính xác bảng dbo.AccountToken trong MyCropDiary_SQLServer.sql
@Entity
@Table(name = "AccountToken", schema = "dbo")
public class AccountToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TokenID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "UserID", nullable = false)
    private AppUser user;

    @Column(name = "TokenHash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "TokenType", nullable = false, length = 30)
    private String tokenType;

    @Column(name = "ExpiresAt", nullable = false)
    private Instant expiresAt;

    @Column(name = "UsedAt")
    private Instant usedAt;

    @Column(name = "RevokedAt")
    private Instant revokedAt;

    @Column(name = "CreatedAt", nullable = false, updatable = false)
    private Instant createdAt;

    protected AccountToken() { }

    // [AI_CHANGE] Factory method cho tạo OTP token mới
    public static AccountToken createOtp(AppUser user, String otpHash, Instant expiresAt) {
        AccountToken token = new AccountToken();
        token.user = user;
        token.tokenHash = otpHash;
        token.tokenType = "EMAIL_OTP";
        token.expiresAt = expiresAt;
        token.createdAt = Instant.now();
        return token;
    }

    // [AI_CHANGE] Factory method cho tạo Refresh Token mới
    public static AccountToken createRefreshToken(AppUser user, String tokenHash, Instant expiresAt) {
        AccountToken token = new AccountToken();
        token.user = user;
        token.tokenHash = tokenHash;
        token.tokenType = "REFRESH_TOKEN";
        token.expiresAt = expiresAt;
        token.createdAt = Instant.now();
        return token;
    }

    // [AI_CHANGE] Root cause: UC-06 cần OTP riêng cho luồng quên mật khẩu (PASSWORD_RESET)
    // [AI_CHANGE] Mechanism: Factory method tạo token type PASSWORD_RESET, tách biệt với EMAIL_OTP đăng ký
    public static AccountToken createPasswordResetOtp(AppUser user, String otpHash, Instant expiresAt) {
        AccountToken token = new AccountToken();
        token.user = user;
        token.tokenHash = otpHash;
        token.tokenType = "PASSWORD_RESET";
        token.expiresAt = expiresAt;
        token.createdAt = Instant.now();
        return token;
    }

    // --- Getters ---
    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public String getTokenType() { return tokenType; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getCreatedAt() { return createdAt; }

    // --- Business methods ---
    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    public boolean isUsed() { return usedAt != null; }
    public boolean isRevoked() { return revokedAt != null; }
    public boolean isValid() { return !isExpired() && !isUsed() && !isRevoked(); }

    public void markUsed() { this.usedAt = Instant.now(); }
    public void revoke() { this.revokedAt = Instant.now(); }
}
