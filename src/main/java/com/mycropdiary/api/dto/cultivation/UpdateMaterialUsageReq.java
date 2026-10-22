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

        @Min(value = 0, message = "Thời gian cách ly phải >= 0")
        Integer safetyIntervalDays,

        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
        String notes
) {}
