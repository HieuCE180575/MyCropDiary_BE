package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "\"User\"")
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", updatable = false, nullable = false)
    private Integer id;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "password_hash", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String passwordHash;

    @Column(name = "avatar_url", columnDefinition = "NVARCHAR(MAX)")
    private String avatarUrl;

    @Column(name = "system_role", nullable = false, length = 20)
    private String systemRole = "user";

    @Column(name = "account_status", nullable = false, length = 20)
    private String accountStatus = "pending";

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    public User() { }

    public User(String email, String passwordHash, String fullName, String phone, String systemRole, String accountStatus) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phone = phone;
        if (systemRole != null) {
            this.systemRole = systemRole.toLowerCase();
        }
        if (accountStatus != null) {
            this.accountStatus = accountStatus.toLowerCase();
        }
    }

    public User(String email, String passwordHash, String fullName, String phone) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phone = phone;
        this.systemRole = "user";
        this.accountStatus = "pending";
    }

    // --- Getters & Setters ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    // Alias for phone
    public String getPhoneNumber() { return phone; }
    public void setPhoneNumber(String phoneNumber) { this.phone = phoneNumber; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getSystemRole() { return systemRole; }
    public void setSystemRole(String systemRole) {
        this.systemRole = systemRole != null ? systemRole.toLowerCase() : "user";
    }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus != null ? accountStatus.toLowerCase() : "pending";
    }

    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(Instant emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }

    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public boolean isActive() { return "active".equalsIgnoreCase(accountStatus); }
    public boolean isPending() { return "pending".equalsIgnoreCase(accountStatus); }
    public boolean isLocked() { return "locked".equalsIgnoreCase(accountStatus); }
    public boolean isAdmin() { return "admin".equalsIgnoreCase(systemRole); }
}
