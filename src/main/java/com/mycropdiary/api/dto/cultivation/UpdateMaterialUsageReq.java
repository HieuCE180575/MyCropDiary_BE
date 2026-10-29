package com.mycropdiary.api.dto.cultivation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO yêu cầu cập nhật nhật ký sử dụng vật tư (UC-25.3).
 */
public record UpdateMaterialUsageReq(
        Integer materialId,

        LocalDateTime usedAt,

        @DecimalMin(value = "0.0001", message = "Số lượng sử dụng phải lớn hơn 0")
        BigDecimal quantity,

        @Size(max = 30, message = "Đơn vị tính tối đa 30 ký tự")
        String unit,

        @Size(max = 100, message = "Liều lượng tối đa 100 ký tự")
        String dosage,

        @Size(max = 200, message = "Phương thức sử dụng tối đa 200 ký tự")
        String method,

        @Size(max = 60, message = "Dạng vật tư tối đa 60 ký tự")
        String materialForm,

        @DecimalMin(value = "0.0001", message = "Diện tích áp dụng phải lớn hơn 0")
        BigDecimal appliedArea,

        @Size(max = 20, message = "Đơn vị diện tích tối đa 20 ký tự")
        String appliedAreaUnit,

        @Min(value = 1, message = "Số cây áp dụng phải lớn hơn 0")
        Integer appliedPlantCount,

        @Min(value = 0, message = "Thời gian cách ly phải >= 0")
        Integer safetyIntervalDays,

        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
        String notes
) {
    public UpdateMaterialUsageReq(
            Integer materialId,
            LocalDateTime usedAt,
            BigDecimal quantity,
            String unit,
            String dosage,
            String method,
            Integer safetyIntervalDays,
            String notes
    ) {
        this(materialId, usedAt, quantity, unit, dosage, method, null, null, null, null, safetyIntervalDays, notes);
    }
}
