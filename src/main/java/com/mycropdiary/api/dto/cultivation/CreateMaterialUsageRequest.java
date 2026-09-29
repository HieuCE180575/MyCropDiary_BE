package com.mycropdiary.api.dto.cultivation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO yêu cầu tạo mới nhật ký sử dụng vật tư (UC-25).
 */
public record CreateMaterialUsageRequest(
        @NotNull(message = "Farming activity ID is required")
        Long farmingActivityId,

        @NotNull(message = "Material ID is required")
        Long materialId,

        Long inputPurchaseDetailId,

        @NotNull(message = "Used at timestamp is required")
        LocalDateTime usedAt,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", message = "Quantity must be greater than 0")
        BigDecimal quantity,

        @NotBlank(message = "Unit is required")
        @Size(max = 30, message = "Unit must not exceed 30 characters")
        String unit,

        @Size(max = 100, message = "Dosage must not exceed 100 characters")
        String dosage,

        @Size(max = 200, message = "Method must not exceed 200 characters")
        String method,

        @Min(value = 0, message = "Safety interval days must be non-negative")
        Integer safetyIntervalDays,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters")
        String notes
) {}
