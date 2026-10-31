package com.mycropdiary.api.dto.procurement;

import com.mycropdiary.api.entity.cultivation.Supplier;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Thông tin nhà cung cấp (Supplier Details)")
public record SupplierResp(
        Integer supplierId,
        Integer farmId,
        String supplierName,
        String contactName,
        String phone,
        String email,
        String address,
        String notes,
        Instant createdAt,
        Long totalPurchasesCount
) {
    public static SupplierResp fromEntity(Supplier entity, Long purchaseCount) {
        if (entity == null) return null;
        return new SupplierResp(
                entity.getId(),
                entity.getFarm().getId(),
                entity.getSupplierName(),
                entity.getContactName(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getAddress(),
                entity.getNotes(),
                entity.getCreatedAt(),
                purchaseCount != null ? purchaseCount : 0L
        );
    }
}
