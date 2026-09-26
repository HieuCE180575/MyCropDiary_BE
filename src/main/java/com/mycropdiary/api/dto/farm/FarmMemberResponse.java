package com.mycropdiary.api.dto.farm;

import java.time.LocalDate;

/**
 * DTO phản hồi thông tin thành viên trang trại.
 */
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
