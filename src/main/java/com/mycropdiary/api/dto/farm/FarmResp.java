package com.mycropdiary.api.dto.farm;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO phản hồi chi tiết thông tin trang trại.
 */
public record FarmResp(
        Integer id,
        String farmName,
        String description,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String boundaryCoordinates,
        String coverImageUrl,
        String status,
        Instant createdAt,
        Instant updatedAt,
        String currentUserRole
) {
    public FarmResp(
            Integer id,
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
        this(id, farmName, description, address, latitude, longitude, null, coverImageUrl, status, createdAt, updatedAt, currentUserRole);
    }

    public String farmCode() { return id != null ? id.toString() : null; }
    public String addressLine() { return address; }
    public BigDecimal totalAreaM2() { return null; }
    public String province() { return null; }
    public String district() { return null; }
    public String ward() { return null; }
    public Integer registrationId() { return null; }
}
