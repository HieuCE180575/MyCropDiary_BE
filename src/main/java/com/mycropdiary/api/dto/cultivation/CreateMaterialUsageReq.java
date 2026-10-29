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
public record CreateMaterialUsageReq(
        @NotNull(message = "Farming activity ID is required")
        Integer farmingActivityId,

        @NotNull(message = "Material ID is required")
        Integer materialId,

        Integer inputPurchaseDetailId,

        Integer blockId,

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

        @Size(max = 60, message = "Dạng vật tư tối đa 60 ký tự")
        String materialForm,

        @DecimalMin(value = "0.0001", message = "Diện tích áp dụng phải lớn hơn 0")
        BigDecimal appliedArea,

        @Size(max = 20, message = "Đơn vị diện tích tối đa 20 ký tự")
        String appliedAreaUnit,

        @Min(value = 1, message = "Số cây áp dụng phải lớn hơn 0")
        Integer appliedPlantCount,

        @Min(value = 0, message = "Safety interval days must be non-negative")
        Integer safetyIntervalDays,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters")
        String notes
) {
    public CreateMaterialUsageReq(
            Integer farmingActivityId,
            Integer materialId,
            Integer inputPurchaseDetailId,
            Integer blockId,
            LocalDateTime usedAt,
            BigDecimal quantity,
            String unit,
            String dosage,
            String method,
            Integer safetyIntervalDays,
            String notes
    ) {
        this(farmingActivityId, materialId, inputPurchaseDetailId, blockId, usedAt, quantity, unit, dosage, method, null, null, null, null, safetyIntervalDays, notes);
    }

    public CreateMaterialUsageReq(
            Integer farmingActivityId,
            Integer materialId,
            Integer inputPurchaseDetailId,
            LocalDateTime usedAt,
            BigDecimal quantity,
            String unit,
            String dosage,
            String method,
            Integer safetyIntervalDays,
            String notes
    ) {
        this(farmingActivityId, materialId, inputPurchaseDetailId, null, usedAt, quantity, unit, dosage, method, null, null, null, null, safetyIntervalDays, notes);
    }
}
