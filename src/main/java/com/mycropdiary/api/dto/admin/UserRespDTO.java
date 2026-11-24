package com.mycropdiary.api.dto.admin;

import com.mycropdiary.api.entity.User;
import java.time.Instant;

public record UserRespDTO(
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
    public UserRespDTO(
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

    public static UserRespDTO fromEntity(User user) {
        return fromEntity(user, null, null);
    }

    public static UserRespDTO fromEntity(User user, String farmsSummary, String zonesSummary) {
        return new UserRespDTO(
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
