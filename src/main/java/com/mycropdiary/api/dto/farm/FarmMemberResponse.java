package com.mycropdiary.api.dto.farm;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * DTO phản hồi thông tin thành viên trang trại.
 */
public record FarmMemberResponse(
        UUID id,
        UUID farmId,
        UUID userId,
        String fullName,
        String email,
        String farmRole,
        String jobTitle,
        Instant joinedAt,
        String status
) {
    public FarmMemberResponse(UUID id, UUID farmId, UUID userId, String fullName, String email, String farmRole, String jobTitle, LocalDate joinedAt, String status) {
        this(id, farmId, userId, fullName, email, farmRole, jobTitle, joinedAt != null ? joinedAt.atStartOfDay(ZoneOffset.UTC).toInstant() : null, status);
    }
}
