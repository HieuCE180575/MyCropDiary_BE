package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.warehouse.InventoryRespDTO;
import com.mycropdiary.api.dto.warehouse.StockInReqDTO;
import com.mycropdiary.api.dto.warehouse.UpdateInventoryReqDTO;
import com.mycropdiary.api.entity.cultivation.Inventory;
import com.mycropdiary.api.entity.cultivation.Material;
import com.mycropdiary.api.entity.cultivation.Warehouse;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.InventoryMapper;
import com.mycropdiary.api.repository.cultivation.InventoryRepository;
import com.mycropdiary.api.repository.cultivation.MaterialRepository;
import com.mycropdiary.api.repository.cultivation.WarehouseRepository;
import com.mycropdiary.api.service.cultivation.InventoryService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final MaterialRepository materialRepository;
    private final FarmAccessGuard accessGuard;
    private final InventoryMapper inventoryMapper;

    @Autowired
    public InventoryServiceImpl(
            InventoryRepository inventoryRepository,
            WarehouseRepository warehouseRepository,
            MaterialRepository materialRepository,
            FarmAccessGuard accessGuard,
            InventoryMapper inventoryMapper) {
        this.inventoryRepository = inventoryRepository;
        this.warehouseRepository = warehouseRepository;
        this.materialRepository = materialRepository;
        this.accessGuard = accessGuard;
        this.inventoryMapper = inventoryMapper != null ? inventoryMapper : new InventoryMapper();
    }

    public InventoryServiceImpl(
            InventoryRepository inventoryRepository,
            WarehouseRepository warehouseRepository,
            MaterialRepository materialRepository,
            FarmAccessGuard accessGuard) {
        this(inventoryRepository, warehouseRepository, materialRepository, accessGuard, new InventoryMapper());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryRespDTO> getInventoriesByFarm(Integer userId, Integer farmId, Integer warehouseId, String keyword) {
        accessGuard.requireFarmAccess(userId, farmId);
        List<Inventory> list = (warehouseId != null)
                ? inventoryRepository.findByWarehouseId(warehouseId)
                : inventoryRepository.findByFarmId(farmId);

        if (keyword != null && !keyword.trim().isEmpty()) {
            String q = keyword.trim().toLowerCase();
            list = list.stream()
                    .filter(i -> (i.getMaterial() != null
                            && i.getMaterial().getMaterialName().toLowerCase().contains(q))
                            || (i.getWarehouse() != null
                            && i.getWarehouse().getWarehouseName().toLowerCase().contains(q)))
                    .toList();
        }

        return inventoryMapper.toResponseList(list);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryRespDTO getInventoryDetails(Integer userId, Integer inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi tồn kho ID=" + inventoryId + " không tồn tại."));
        accessGuard.requireFarmAccess(userId, inventory.getWarehouse().getFarm().getId());
        return inventoryMapper.toResponse(inventory);
    }

    @Override
    public InventoryRespDTO createInventory(Integer userId, Integer farmId, StockInReqDTO request) {
        accessGuard.requireFarmOwner(userId, farmId);

        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Kho hàng ID=" + request.warehouseId() + " không tồn tại."));
        if (!warehouse.getFarm().getId().equals(farmId)) {
            throw new BadRequestException("Kho hàng không thuộc trang trại này.");
        }

        Material material = materialRepository.findByIdAndFarmId(request.materialId(), farmId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vật tư ID=" + request.materialId() + " không tồn tại trong trang trại."));

        if (inventoryRepository.findByWarehouseIdAndMaterialId(request.warehouseId(), request.materialId()).isPresent()) {
            throw new BadRequestException(
                    "Vật tư này đã tồn tại trong kho. Hãy cập nhật số lượng tồn thay vì thêm mới.");
        }

        Inventory inventory = new Inventory(
                warehouse,
                material,
                request.quantityOnHand(),
                request.reorderLevel());
        Inventory saved = inventoryRepository.save(inventory);
        return inventoryMapper.toResponse(saved);
    }

    @Override
    public InventoryRespDTO updateInventory(Integer userId, Integer inventoryId, UpdateInventoryReqDTO request) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi tồn kho ID=" + inventoryId + " không tồn tại."));
        accessGuard.requireFarmOwner(userId, inventory.getWarehouse().getFarm().getId());

        inventory.setQuantityOnHand(request.quantityOnHand());
        inventory.setReorderLevel(request.reorderLevel());
        Inventory saved = inventoryRepository.save(inventory);
        return inventoryMapper.toResponse(saved);
    }

    @Override
    public void deleteInventory(Integer userId, Integer inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi tồn kho ID=" + inventoryId + " không tồn tại."));
        accessGuard.requireFarmOwner(userId, inventory.getWarehouse().getFarm().getId());
        inventoryRepository.delete(inventory);
    }
}
