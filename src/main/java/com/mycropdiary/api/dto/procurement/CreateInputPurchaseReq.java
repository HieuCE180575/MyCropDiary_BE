package com.mycropdiary.api.dto.procurement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Yêu cầu ghi nhận phiếu mua vật tư (Record Input Purchase)")
public record CreateInputPurchaseReq(
        @Schema(description = "ID nhà cung cấp", example = "1")
        Integer supplierId,

        @Schema(description = "Ngày mua / hóa đơn", example = "2026-01-20")
        LocalDate purchaseDate,

        @Schema(description = "Số hóa đơn / Số chứng từ", example = "PO-2026-002")
        String referenceNo,

        @Schema(description = "Ảnh chụp hóa đơn / biên lai")
        String receiptImageUrl,

        @Schema(description = "Số phiếu nhập kho liên kết (nếu có)", example = "NK-2026-015")
        String warehouseReceiptNo,

        @Schema(description = "Ghi chú phiếu mua", example = "Nhập vật tư phân bón hòa tan chuẩn bị cho mùa vụ mới")
        String notes,

        @Schema(description = "Danh sách các mặt hàng vật tư trong phiếu mua")
        @NotEmpty(message = "Phiếu mua phải có ít nhất một mặt hàng vật tư")
        @Valid
        List<PurchaseItemReq> items
) {
}

