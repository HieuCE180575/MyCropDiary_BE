package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.warehouse.InventoryRespDTO;
import com.mycropdiary.api.dto.warehouse.StockInReqDTO;
import com.mycropdiary.api.dto.warehouse.UpdateInventoryReqDTO;

import java.util.List;

public interface InventoryService {
    List<InventoryRespDTO> getInventoriesByFarm(Integer userId, Integer farmId, Integer warehouseId, String keyword);
    InventoryRespDTO getInventoryDetails(Integer userId, Integer inventoryId);
    InventoryRespDTO createInventory(Integer userId, Integer farmId, StockInReqDTO request);
    InventoryRespDTO updateInventory(Integer userId, Integer inventoryId, UpdateInventoryReqDTO request);
    void deleteInventory(Integer userId, Integer inventoryId);
}
