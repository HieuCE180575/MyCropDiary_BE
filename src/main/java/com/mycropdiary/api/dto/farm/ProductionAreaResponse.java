package com.mycropdiary.api.dto.farm;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO phản hồi thông tin chi tiết Vùng sản xuất.
 */
public record ProductionAreaResponse(
        Long id,
        Long farmId,
        String areaCode,
        String areaName,
        BigDecimal areaM2,
        String description,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
