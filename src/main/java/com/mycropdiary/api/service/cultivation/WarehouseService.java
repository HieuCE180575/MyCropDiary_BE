package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.warehouse.CreateWarehouseReqDTO;
import com.mycropdiary.api.dto.warehouse.UpdateWarehouseReqDTO;
import com.mycropdiary.api.dto.warehouse.WarehouseOverviewRespDTO;
import com.mycropdiary.api.dto.warehouse.WarehouseRespDTO;

import java.util.List;

public interface WarehouseService {
    List<WarehouseRespDTO> getWarehousesByFarm(Integer userId, Integer farmId);
    WarehouseRespDTO getWarehouseDetails(Integer userId, Integer warehouseId);
    WarehouseRespDTO createWarehouse(Integer userId, Integer farmId, CreateWarehouseReqDTO request);
    WarehouseRespDTO updateWarehouse(Integer userId, Integer warehouseId, UpdateWarehouseReqDTO request);
    void deleteWarehouse(Integer userId, Integer warehouseId);
    WarehouseOverviewRespDTO getWarehouseOverview(Integer currentUserId, Integer warehouseId);
}
