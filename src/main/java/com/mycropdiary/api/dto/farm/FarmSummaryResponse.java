package com.mycropdiary.api.dto.farm;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO phản hồi danh sách tóm tắt trang trại.
 */
public record FarmSummaryResponse(
        UUID id,
        String farmName,
        String description,
        String address,
        String status,
        Instant createdAt,
        String currentUserRole
) {
    public FarmSummaryResponse(
            UUID id,
            String farmName,
            String description,
            String address,
            String status,
            Instant createdAt
    ) {
        this(id, farmName, description, address, status, createdAt, null);
    }

    public String farmCode() { return id != null ? id.toString() : null; }
    public String addressLine() { return address; }
    public String province() { return null; }
    public String district() { return null; }
    public String ward() { return null; }
    public BigDecimal totalAreaM2() { return null; }
    public String code() { return farmCode(); }
    public String name() { return farmName; }
}
