package com.mycropdiary.api.dto.procurement;

import com.mycropdiary.api.entity.cultivation.InputPurchase;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Thông tin chi tiết phiếu mua vật tư (Input Purchase Details)")
public record InputPurchaseResp(
        Integer purchaseId,
        Integer farmId,
        Integer supplierId,
        String supplierName,
        Integer purchasedByMemberId,
        String purchasedByName,
        LocalDate purchaseDate,
        String referenceNo,
        BigDecimal totalAmount,
        String receiptImageUrl,
        String warehouseReceiptNo,
        String notes,
        Instant createdAt,
        List<PurchaseItemResp> items
) {
    public static InputPurchaseResp fromEntity(InputPurchase entity, List<PurchaseItemResp> items) {
        if (entity == null) return null;
        Integer suppId = entity.getSupplier() != null ? entity.getSupplier().getId() : null;
        String suppName = entity.getSupplier() != null ? entity.getSupplier().getSupplierName() : null;

        Integer memberId = entity.getPurchasedBy() != null ? entity.getPurchasedBy().getId() : null;
        String memberName = entity.getPurchasedBy() != null && entity.getPurchasedBy().getUser() != null
                ? entity.getPurchasedBy().getUser().getFullName() : null;

        return new InputPurchaseResp(
                entity.getId(),
                entity.getFarm().getId(),
                suppId,
                suppName,
                memberId,
                memberName,
                entity.getPurchaseDate(),
                entity.getReferenceNo(),
                entity.getTotalAmount(),
                entity.getReceiptImageUrl(),
                entity.getWarehouseReceiptNo(),
                entity.getNotes(),
                entity.getCreatedAt(),
                items != null ? items : List.of()
        );
    }
}

