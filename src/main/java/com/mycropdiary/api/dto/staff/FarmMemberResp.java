package com.mycropdiary.api.dto.staff;

import java.time.Instant;

public record FarmMemberResp(
        Integer id,
        Integer farmId,
        String farmName,
        Integer userId,
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
