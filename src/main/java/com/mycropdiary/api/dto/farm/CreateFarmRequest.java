package com.mycropdiary.api.dto.farm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateFarmRequest(
        @NotBlank(message = "Farm code is required")
        @Size(max = 30, message = "Farm code must not exceed 30 characters")
        String farmCode,

        @NotBlank(message = "Farm name is required")
        @Size(max = 200, message = "Farm name must not exceed 200 characters")
        String farmName,

        @Size(max = 300, message = "Address line must not exceed 300 characters")
        String addressLine,

        String province,
        String district,
        String ward,

        BigDecimal latitude,
        BigDecimal longitude,

        @Positive(message = "Total area must be positive")
        BigDecimal totalAreaM2
) {}
