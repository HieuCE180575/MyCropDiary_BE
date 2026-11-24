package com.mycropdiary.api.dto.farm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateFarmReqDTO(
        @NotBlank(message = "Farm name is required")
        @Size(max = 160, message = "Farm name must not exceed 160 characters")
        String farmName,

        String description,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String boundaryCoordinates,
        String coverImageUrl,
        String farmMapUrl
) {
    public CreateFarmReqDTO(String farmName, String description, String address, BigDecimal latitude, BigDecimal longitude, String boundaryCoordinates, String coverImageUrl) {
        this(farmName, description, address, latitude, longitude, boundaryCoordinates, coverImageUrl, null);
    }

    public CreateFarmReqDTO(String farmName, String description, String address, BigDecimal latitude, BigDecimal longitude, String coverImageUrl) {
        this(farmName, description, address, latitude, longitude, null, coverImageUrl, null);
    }

    // Compatibility constructor for existing callers
    public CreateFarmReqDTO(String farmCode, String farmName, String addressLine, String province, String district, String ward, BigDecimal latitude, BigDecimal longitude, BigDecimal totalAreaM2) {
        this(farmName, null, addressLine != null ? addressLine : (province != null ? province : ""), latitude, longitude, null, null, null);
    }
}
