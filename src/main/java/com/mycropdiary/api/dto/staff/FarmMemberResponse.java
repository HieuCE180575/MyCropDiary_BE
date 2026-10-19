package com.mycropdiary.api.dto.staff;

import java.time.Instant;
import java.util.UUID;

public record FarmMemberResponse(
        UUID id,
        UUID farmId,
        String farmName,
        UUID userId,
        String fullName,
        String email,
        String phoneNumber,
        String farmRole,
        String jobTitle,
        Instant joinedAt,
        Instant leftAt,
        String status,
        int activeAssignmentCount
) {
}
