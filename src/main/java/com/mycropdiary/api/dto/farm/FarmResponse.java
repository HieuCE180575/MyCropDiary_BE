package com.mycropdiary.api.dto.farm;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO phản hồi chi tiết thông tin trang trại.
 */
public record FarmResponse(
        UUID id,
        String farmName,
        String description,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String coverImageUrl,
        String status,
        Instant createdAt,
        Instant updatedAt,
        String currentUserRole
) {
    public String farmCode() { return id != null ? id.toString() : null; }
    public String addressLine() { return address; }
    public BigDecimal totalAreaM2() { return null; }
    public String province() { return null; }
    public String district() { return null; }
    public String ward() { return null; }
    public UUID registrationId() { return null; }
}
