package com.mycropdiary.api.dto.cultivation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssignCropSeasonBlockReq(
        @NotNull(message = "Mã lô đất (blockId) không được để trống")
        Integer blockId,

        @DecimalMin(value = "0.01", message = "Diện tích gieo trồng phải lớn hơn 0")
        BigDecimal plantedArea,

        @Size(max = 20, message = "Đơn vị diện tích không được vượt quá 20 ký tự")
        String areaUnit,

        LocalDate plantedAt,

        LocalDate expectedHarvestAt
) {
}
