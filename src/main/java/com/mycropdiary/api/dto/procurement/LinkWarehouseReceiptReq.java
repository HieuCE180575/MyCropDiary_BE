package com.mycropdiary.api.dto.procurement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Yêu cầu liên kết phiếu mua vật tư với phiếu nhập kho (Link Input Purchase to Warehouse Receipt)")
public record LinkWarehouseReceiptReq(
        @Schema(description = "Số phiếu nhập kho lưu trữ", example = "NK-2026-015")
        @NotBlank(message = "Mã số phiếu nhập kho không được để trống")
        String warehouseReceiptNo,

        @Schema(description = "ID kho nhập hàng", example = "1")
        Integer warehouseId,

        @Schema(description = "Ghi chú quá trình nhập kho")
        String notes
) {
}
