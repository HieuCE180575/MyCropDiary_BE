package com.mycropdiary.api.dto.user;

import com.mycropdiary.api.entity.User;
import java.time.Instant;

/**
 * DTO trả về thông tin hồ sơ tài khoản người dùng hiện tại (UC-03 / Profile).
 */
public record UserProfileResp(
        Integer userId,
        String email,
        String fullName,
        String phone,
        String avatarUrl,
        String systemRole,
        String accountStatus,
        Instant emailVerifiedAt,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserProfileResp fromEntity(User user) {
        return new UserProfileResp(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAvatarUrl(),
                user.getSystemRole() != null ? user.getSystemRole().toUpperCase() : "USER",
                user.getAccountStatus() != null ? user.getAccountStatus().toUpperCase() : "PENDING",
                user.getEmailVerifiedAt(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
