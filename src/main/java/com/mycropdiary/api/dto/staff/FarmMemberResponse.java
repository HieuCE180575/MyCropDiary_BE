package com.mycropdiary.api.dto.staff;

import java.time.LocalDate;

public record FarmMemberResponse(
        Long id,
        Long farmId,
        String farmName,
        Long userId,
        String fullName,
        String email,
        String phoneNumber,
        String farmRole,
        String jobTitle,
        LocalDate joinedAt,
        LocalDate leftAt,
        String status,
        int activeAssignmentCount
) {
}
