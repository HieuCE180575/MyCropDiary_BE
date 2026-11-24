package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.warehouse.InventoryItemResp;
import com.mycropdiary.api.dto.warehouse.WarehouseResp;
import com.mycropdiary.api.entity.cultivation.Inventory;
import com.mycropdiary.api.entity.cultivation.Warehouse;
import org.springframework.stereotype.Component;

/**
 * Lớp chuyển đổi dữ liệu (Mapper) giữa Entities Warehouse / Inventory và DTOs.
 */
@Component
public class WarehouseMapper {

    public WarehouseResp toWarehouseResp(Warehouse w) {
        int count = w.getInventories() != null ? w.getInventories().size() : 0;
        return new WarehouseResp(
                w.getId(),
                w.getFarm().getId(),
                w.getFarm().getFarmName(),
                w.getWarehouseName(),
                w.getWarehouseType() != null ? w.getWarehouseType() : "kho_chung",
                w.getAddress(),
                w.getImageUrl(),
                count,
                w.getCreatedAt()
        );
    }

    public InventoryItemResp toInventoryItemResp(Inventory i) {
        String matType = i.getMaterial().getMaterialType();
        boolean lowStock = i.getQuantityOnHand().compareTo(i.getReorderLevel()) <= 0;
        return new InventoryItemResp(
                i.getId(),
                i.getWarehouse().getId(),
                i.getWarehouse().getWarehouseName(),
                i.getMaterial().getId(),
                i.getMaterial().getMaterialName(),
                matType,
                i.getMaterial().getUnit(),
                i.getQuantityOnHand(),
                i.getReorderLevel(),
                lowStock,
                i.getUpdatedAt()
        );
    }
}
