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
        Instant updatedAt
) {
    public static UserResp fromEntity(User user) {
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
                user.getUpdatedAt()
        );
    }
}
