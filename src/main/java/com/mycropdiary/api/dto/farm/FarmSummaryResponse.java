package com.mycropdiary.api.dto.farm;

import java.math.BigDecimal;

/**
 * DTO phản hồi danh sách tóm tắt trang trại.
 */
public record FarmSummaryResponse(
        Long id,
        String farmCode,
        String farmName,
        String province,
        String district,
        BigDecimal totalAreaM2,
        String status,
        String currentUserRole
) {}
