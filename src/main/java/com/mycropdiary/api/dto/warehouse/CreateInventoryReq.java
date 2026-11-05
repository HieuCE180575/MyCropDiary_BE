package com.mycropdiary.api.dto.warehouse;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateInventoryReq(
        @NotNull(message = "Kho hàng không được để trống")
        Integer warehouseId,

        @NotNull(message = "Vật tư không được để trống")
        Integer materialId,

        @NotNull(message = "Số lượng tồn kho không được để trống")
        @DecimalMin(value = "0.0", message = "Số lượng tồn kho không được âm")
        BigDecimal quantityOnHand,

        @NotNull(message = "Mức cảnh báo đặt hàng không được để trống")
        @DecimalMin(value = "0.0", message = "Mức cảnh báo đặt hàng không được âm")
        BigDecimal reorderLevel
) {
}

