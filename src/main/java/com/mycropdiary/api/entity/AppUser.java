package com.mycropdiary.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

// [AI_CHANGE] Root cause: AppUser cần setter và constructor builder để AuthService tạo tài khoản mới
// [AI_CHANGE] Mechanism: Thêm EmailVerifiedAt, setter cho các field mutable, static builder method
@Entity
@Table(name = "AppUser", schema = "dbo")
public class AppUser extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "UserID")
    private Long id;

    @Column(name = "Email", nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "PasswordHash", nullable = false)
    private String passwordHash;

    @Column(name = "FullName", nullable = false, length = 150)
    private String fullName;

    @Column(name = "PhoneNumber", length = 20)
    private String phoneNumber;

    @Column(name = "SystemRole", nullable = false, length = 20)
    private String systemRole = "USER";

    @Column(name = "AccountStatus", nullable = false, length = 20)
    private String accountStatus = "PENDING";

    @Column(name = "EmailVerifiedAt")
    private Instant emailVerifiedAt;

    protected AppUser() { }

    // [AI_CHANGE] Constructor cho việc đăng ký tài khoản mới
    public AppUser(String email, String passwordHash, String fullName, String phoneNumber) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.systemRole = "USER";
        this.accountStatus = "PENDING";
    }

    // --- Getters ---
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getSystemRole() { return systemRole; }
    public String getAccountStatus() { return accountStatus; }
    public Instant getEmailVerifiedAt() { return emailVerifiedAt; }

    // --- Setters (chỉ cho các field có thể thay đổi qua nghiệp vụ) ---
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
    public void setSystemRole(String systemRole) { this.systemRole = systemRole; }
    public void setEmailVerifiedAt(Instant emailVerifiedAt) { this.emailVerifiedAt = emailVerifiedAt; }

    // [AI_CHANGE] Kiểm tra nhanh trạng thái tài khoản
    public boolean isActive() { return "ACTIVE".equals(accountStatus); }
    public boolean isPending() { return "PENDING".equals(accountStatus); }
    public boolean isLocked() { return "LOCKED".equals(accountStatus); }
}
