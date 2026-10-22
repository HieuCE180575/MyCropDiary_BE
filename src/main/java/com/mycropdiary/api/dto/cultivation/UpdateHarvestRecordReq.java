package com.mycropdiary.api.dto.cultivation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO yêu cầu cập nhật bản ghi thu hoạch (UC-26.3).
 */
public record UpdateHarvestRecordReq(
        Integer blockId,

        LocalDateTime harvestedAt,

        @DecimalMin(value = "0.0001", message = "Sản lượng thu hoạch phải lớn hơn 0")
        BigDecimal quantity,

        @Size(max = 30, message = "Đơn vị tính tối đa 30 ký tự")
        String unit,

        @Size(max = 50, message = "Chất lượng tối đa 50 ký tự")
        String qualityGrade,

        @Size(max = 200, message = "Vị trí lưu kho tối đa 200 ký tự")
        String storageLocation,

        @Size(max = 1000, message = "Ghi chú tối đa 1000 ký tự")
        String notes
) {
    public UpdateHarvestRecordReq(
            LocalDateTime harvestedAt,
            BigDecimal quantity,
            String unit,
            String qualityGrade,
            String storageLocation,
            String notes
    ) {
        this(null, harvestedAt, quantity, unit, qualityGrade, storageLocation, notes);
    }
}
