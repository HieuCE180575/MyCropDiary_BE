package com.mycropdiary.api.dto.procurement;

import com.mycropdiary.api.entity.cultivation.InputPurchaseItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Thông tin chi tiết mặt hàng đã mua (Purchase Item Details)")
public record PurchaseItemResp(
        Integer purchaseItemId,
        Integer materialId,
        String materialName,
        String materialType,
        String unit,
        Integer warehouseId,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal lineTotal
) {
    public static PurchaseItemResp fromEntity(InputPurchaseItem item) {
        if (item == null) return null;
        return new PurchaseItemResp(
                item.getId(),
                item.getMaterial().getId(),
                item.getMaterial().getMaterialName(),
                item.getMaterial().getMaterialType(),
                item.getMaterial().getUnit(),
                item.getWarehouseId(),
                item.getQuantity(),
                item.getUnitCost(),
                item.getLineTotal()
        );
    }
}

