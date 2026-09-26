package com.mycropdiary.api.dto.farm;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO phản hồi chi tiết thông tin trang trại.
 */
public record FarmResponse(
        Long id,
        Long registrationId,
        String farmCode,
        String farmName,
        String addressLine,
        String province,
        String district,
        String ward,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal totalAreaM2,
        String status,
        Instant createdAt,
        Instant updatedAt,
        String currentUserRole
) {}
