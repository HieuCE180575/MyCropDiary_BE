package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "AccountToken")
public class AccountToken {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "token_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "token_type", nullable = false, length = 30)
    private String tokenType;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public AccountToken() { }

    public static AccountToken createOtp(User user, String otpHash, Instant expiresAt) {
        AccountToken token = new AccountToken();
        token.user = user;
        token.tokenHash = otpHash;
        token.tokenType = "email_verification";
        token.expiresAt = expiresAt;
        token.createdAt = Instant.now();
        return token;
    }

    public static AccountToken createRefreshToken(User user, String tokenHash, Instant expiresAt) {
        AccountToken token = new AccountToken();
        token.user = user;
        token.tokenHash = tokenHash;
        token.tokenType = "refresh";
        token.expiresAt = expiresAt;
        token.createdAt = Instant.now();
        return token;
    }

    public static AccountToken createPasswordResetOtp(User user, String otpHash, Instant expiresAt) {
        AccountToken token = new AccountToken();
        token.user = user;
        token.tokenHash = otpHash;
        token.tokenType = "password_reset";
        token.expiresAt = expiresAt;
        token.createdAt = Instant.now();
        return token;
    }

    // --- Getters & Setters ---
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // --- Business methods ---
    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    public boolean isUsed() { return usedAt != null; }
    public boolean isValid() { return !isExpired() && !isUsed(); }
    public void markUsed() { this.usedAt = Instant.now(); }
    public void revoke() { this.usedAt = Instant.now(); } // marks used since revoked_at is omitted
}
