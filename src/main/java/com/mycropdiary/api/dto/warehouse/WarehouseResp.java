package com.mycropdiary.api.dto.warehouse;

import java.time.Instant;

public record WarehouseResp(
        Integer warehouseId,
        Integer farmId,
        String farmName,
        String warehouseName,
        String warehouseType,
        String address,
        String imageUrl,
        int totalItemCount,
        Instant createdAt
) {
    public WarehouseResp(
            Integer warehouseId,
            Integer farmId,
            String farmName,
            String warehouseName,
            String address,
            String imageUrl,
            int totalItemCount,
            Instant createdAt
    ) {
        this(warehouseId, farmId, farmName, warehouseName, "kho_chung", address, imageUrl, totalItemCount, createdAt);
    }
}

