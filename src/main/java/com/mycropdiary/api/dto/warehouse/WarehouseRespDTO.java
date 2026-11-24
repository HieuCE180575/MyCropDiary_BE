package com.mycropdiary.api.dto.warehouse;

import java.time.Instant;

public record WarehouseRespDTO(
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
    public WarehouseRespDTO(
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

