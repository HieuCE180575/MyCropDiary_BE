package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.warehouse.CreateInventoryReq;
import com.mycropdiary.api.dto.warehouse.CreateWarehouseReq;
import com.mycropdiary.api.dto.warehouse.InventoryItemResp;
import com.mycropdiary.api.dto.warehouse.UpdateInventoryReq;
import com.mycropdiary.api.dto.warehouse.WarehouseResp;

import java.util.List;

public interface WarehouseService {
    List<WarehouseResp> getWarehousesByFarm(Integer userId, Integer farmId);
    WarehouseResp getWarehouseDetails(Integer userId, Integer warehouseId);
    WarehouseResp createWarehouse(Integer userId, Integer farmId, CreateWarehouseReq request);
    WarehouseResp updateWarehouse(Integer userId, Integer warehouseId, CreateWarehouseReq request);
    void deleteWarehouse(Integer userId, Integer warehouseId);

    List<InventoryItemResp> getInventoriesByFarm(Integer userId, Integer farmId, Integer warehouseId, String keyword);
    InventoryItemResp getInventoryDetails(Integer userId, Integer inventoryId);
    InventoryItemResp createInventory(Integer userId, Integer farmId, CreateInventoryReq request);
    InventoryItemResp updateInventory(Integer userId, Integer inventoryId, UpdateInventoryReq request);
    void deleteInventory(Integer userId, Integer inventoryId);
}

