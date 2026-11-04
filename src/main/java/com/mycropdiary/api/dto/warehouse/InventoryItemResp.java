package com.mycropdiary.api.dto.warehouse;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryItemResp(
        Integer inventoryId,
        Integer warehouseId,
        String warehouseName,
        Integer materialId,
        String materialName,
        String materialType,
        String unit,
        BigDecimal quantityOnHand,
        BigDecimal reorderLevel,
        boolean isLowStock,
        Instant updatedAt
) {
}

