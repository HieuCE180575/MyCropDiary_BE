package com.mycropdiary.api.dto.farm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductionAreaRequest(
        @NotBlank(message = "Area code is required")
        @Size(max = 30, message = "Area code must not exceed 30 characters")
        String areaCode,

        @NotBlank(message = "Area name is required")
        @Size(max = 150, message = "Area name must not exceed 150 characters")
        String areaName,

        @Positive(message = "Area size must be positive")
        BigDecimal areaM2,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description
) {}
