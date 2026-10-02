package com.mycropdiary.api.dto.farm;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO phản hồi danh sách tóm tắt trang trại.
 */
public record FarmSummaryResponse(
        Long id,
        String farmCode,
        String farmName,
        String addressLine,
        String province,
        String district,
        String ward,
        BigDecimal totalAreaM2,
        String status,
        Instant createdAt,
        String currentUserRole
) {
    public FarmSummaryResponse(
            Long id,
            String farmCode,
            String farmName,
            String province,
            String district,
            BigDecimal totalAreaM2,
            String status,
            String currentUserRole
    ) {
        this(id, farmCode, farmName, null, province, district, null, totalAreaM2, status, null, currentUserRole);
    }

    public FarmSummaryResponse(
            Long id,
            String farmCode,
            String farmName,
            String addressLine,
            String province,
            String district,
            String ward,
            BigDecimal totalAreaM2,
            String status,
            Instant createdAt
    ) {
        this(id, farmCode, farmName, addressLine, province, district, ward, totalAreaM2, status, createdAt, null);
    }

    public String code() { return farmCode; }
    public String name() { return farmName; }
}
