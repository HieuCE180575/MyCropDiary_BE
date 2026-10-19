package com.mycropdiary.api.dto.farm;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateFarmReq(
        @Size(max = 160, message = "Farm name must not exceed 160 characters")
        String farmName,

        String description,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String coverImageUrl,
        String status
) {
    public UpdateFarmReq(String farmName, String addressLine, String province, String district, String ward, BigDecimal latitude, BigDecimal longitude, BigDecimal totalAreaM2) {
        this(farmName, null, addressLine != null ? addressLine : (province != null ? province : ""), latitude, longitude, null, null);
    }

    public UpdateFarmReq(String farmName, String addressLine, String province, String district, String ward, BigDecimal latitude, BigDecimal longitude, BigDecimal totalAreaM2, String status) {
        this(farmName, null, addressLine != null ? addressLine : (province != null ? province : ""), latitude, longitude, null, status);
    }
}
