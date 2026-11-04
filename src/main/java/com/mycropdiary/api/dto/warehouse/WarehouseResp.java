package com.mycropdiary.api.dto.warehouse;

import java.time.Instant;

public record WarehouseResp(
        Integer warehouseId,
        Integer farmId,
        String farmName,
        String warehouseName,
        String address,
        String imageUrl,
        int totalItemCount,
        Instant createdAt
) {
}

