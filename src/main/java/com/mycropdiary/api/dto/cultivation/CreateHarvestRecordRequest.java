package com.mycropdiary.api.dto.cultivation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO yêu cầu tạo mới bản ghi thu hoạch (UC-26).
 */
public record CreateHarvestRecordRequest(
        @NotNull(message = "Crop season ID is required")
        Long cropSeasonId,

        @Size(max = 60, message = "Harvest lot code must not exceed 60 characters")
        String harvestLotCode,

        @NotNull(message = "Harvested at timestamp is required")
        LocalDateTime harvestedAt,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", message = "Quantity must be greater than 0")
        BigDecimal quantity,

        @NotBlank(message = "Unit is required")
        @Size(max = 30, message = "Unit must not exceed 30 characters")
        String unit,

        @Size(max = 50, message = "Quality grade must not exceed 50 characters")
        String qualityGrade,

        @Size(max = 200, message = "Storage location must not exceed 200 characters")
        String storageLocation,

        @Size(max = 100, message = "Traceability code must not exceed 100 characters")
        String traceabilityCode,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters")
        String notes
) {}
