package com.mycropdiary.api.dto.procurement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Mặt hàng vật tư trong phiếu mua (Purchase Item)")
public record PurchaseItemReq(
        @Schema(description = "ID của vật tư", example = "1")
        @NotNull(message = "Mã vật tư (materialId) không được để trống")
        Integer materialId,

        @Schema(description = "Số lượng mua", example = "300.0")
        @NotNull(message = "Số lượng không được để trống")
        @DecimalMin(value = "0.001", message = "Số lượng phải lớn hơn 0")
        BigDecimal quantity,

        @Schema(description = "Đơn giá (VNĐ)", example = "35000.00")
        @NotNull(message = "Đơn giá không được để trống")
        @DecimalMin(value = "0.00", message = "Đơn giá không được âm")
        BigDecimal unitCost,

        @Schema(description = "ID kho lưu trữ (tùy chọn)", example = "1")
        Integer warehouseId
) {
}

