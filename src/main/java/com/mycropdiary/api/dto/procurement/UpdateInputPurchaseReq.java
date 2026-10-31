package com.mycropdiary.api.dto.procurement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Yêu cầu cập nhật phiếu mua vật tư (Update Input Purchase)")
public record UpdateInputPurchaseReq(
        @Schema(description = "ID nhà cung cấp", example = "1")
        Integer supplierId,

        @Schema(description = "Ngày mua / hóa đơn", example = "2026-01-20")
        LocalDate purchaseDate,

        @Schema(description = "Số hóa đơn / Số chứng từ", example = "PO-2026-002")
        String referenceNo,

        @Schema(description = "Ảnh chụp hóa đơn / biên lai")
        String receiptImageUrl,

        @Schema(description = "Số phiếu nhập kho liên kết", example = "NK-2026-015")
        String warehouseReceiptNo,

        @Schema(description = "Ghi chú phiếu mua", example = "Đã đối chiếu số lượng thực nhận tại kho")
        String notes,

        @Schema(description = "Danh sách cập nhật các mặt hàng vật tư (tùy chọn)")
        @Valid
        List<PurchaseItemReq> items
) {
}
