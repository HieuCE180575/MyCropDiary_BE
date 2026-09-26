package com.mycropdiary.api.dto.farm;

import java.time.LocalDate;

public record FarmMemberResponse(
        Long id,
        Long farmId,
        Long userId,
        String fullName,
        String email,
        String farmRole,
        String jobTitle,
        LocalDate joinedAt,
        String status
) {}
