package com.mycropdiary.api.entity;

import jakarta.persistence.*;

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

    protected AppUser() { }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getSystemRole() { return systemRole; }
    public String getAccountStatus() { return accountStatus; }
}
