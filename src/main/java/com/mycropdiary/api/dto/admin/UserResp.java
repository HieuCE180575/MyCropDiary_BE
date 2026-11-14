package com.mycropdiary.api.dto.admin;

import com.mycropdiary.api.entity.User;
import java.time.Instant;

public record UserResp(
        Integer userId,
        String email,
        String fullName,
        String phone,
        String systemRole,
        String accountStatus,
        Instant emailVerifiedAt,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt,
        String farmsSummary,
        String zonesSummary
) {
    public UserResp(
            Integer userId,
            String email,
            String fullName,
            String phone,
            String systemRole,
            String accountStatus,
            Instant emailVerifiedAt,
            Instant lastLoginAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(userId, email, fullName, phone, systemRole, accountStatus, emailVerifiedAt, lastLoginAt, createdAt, updatedAt, null, null);
    }

    public static UserResp fromEntity(User user) {
        return fromEntity(user, null, null);
    }

    public static UserResp fromEntity(User user, String farmsSummary, String zonesSummary) {
        return new UserResp(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getSystemRole() != null ? user.getSystemRole().toUpperCase() : "USER",
                user.getAccountStatus() != null ? user.getAccountStatus().toUpperCase() : "PENDING",
                user.getEmailVerifiedAt(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                farmsSummary,
                zonesSummary
        );
    }
}
