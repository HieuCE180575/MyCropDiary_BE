package com.mycropdiary.api.dto.warehouse;

import java.util.List;

public record WarehouseOverviewResp(
        Integer warehouseId,
        String warehouseName,
        String farmName,
        String address,
        Integer totalItemCount,
        Integer lowStockCount,
        Integer outOfStockCount,
        List<InventoryItemResp> recentMaterials
) {}
