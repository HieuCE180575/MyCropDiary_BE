package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.warehouse.CreateInventoryReq;
import com.mycropdiary.api.dto.warehouse.CreateWarehouseReq;
import com.mycropdiary.api.dto.warehouse.InventoryItemResp;
import com.mycropdiary.api.dto.warehouse.UpdateInventoryReq;
import com.mycropdiary.api.dto.warehouse.WarehouseResp;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.cultivation.Inventory;
import com.mycropdiary.api.entity.cultivation.Material;
import com.mycropdiary.api.entity.cultivation.Warehouse;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.InventoryRepository;
import com.mycropdiary.api.repository.cultivation.MaterialRepository;
import com.mycropdiary.api.repository.cultivation.WarehouseRepository;
import com.mycropdiary.api.service.cultivation.WarehouseService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final MaterialRepository materialRepository;
    private final FarmRepository farmRepository;
    private final FarmAccessGuard accessGuard;

    public WarehouseServiceImpl(WarehouseRepository warehouseRepository,
                                InventoryRepository inventoryRepository,
                                MaterialRepository materialRepository,
                                FarmRepository farmRepository,
                                FarmAccessGuard accessGuard) {
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
        this.materialRepository = materialRepository;
        this.farmRepository = farmRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseResp> getWarehousesByFarm(Integer userId, Integer farmId) {
        accessGuard.requireFarmAccess(userId, farmId);
        List<Warehouse> list = warehouseRepository.findByFarmIdOrderByWarehouseNameAsc(farmId);
        return list.stream().map(this::toWarehouseResp).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseResp getWarehouseDetails(Integer userId, Integer warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Kho hàng ID=" + warehouseId + " không tồn tại."));
        accessGuard.requireFarmAccess(userId, warehouse.getFarm().getId());
        return toWarehouseResp(warehouse);
    }

    @Override
    public WarehouseResp createWarehouse(Integer userId, Integer farmId, CreateWarehouseReq request) {
        accessGuard.requireFarmAccess(userId, farmId);
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Trang trại không tồn tại."));

        if (warehouseRepository.existsByFarmIdAndWarehouseNameIgnoreCase(farmId, request.warehouseName().trim())) {
            throw new BadRequestException("Tên kho hàng đã tồn tại trong trang trại.");
        }

        Warehouse warehouse = new Warehouse(
                farm,
                request.warehouseName().trim(),
                request.address() != null ? request.address().trim() : null,
                request.imageUrl() != null ? request.imageUrl().trim() : null
        );

        Warehouse saved = warehouseRepository.save(warehouse);
        return toWarehouseResp(saved);
    }

    @Override
    public WarehouseResp updateWarehouse(Integer userId, Integer warehouseId, CreateWarehouseReq request) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Kho hàng ID=" + warehouseId + " không tồn tại."));
        accessGuard.requireFarmOwner(userId, warehouse.getFarm().getId());

        String newName = request.warehouseName().trim();
        if (!warehouse.getWarehouseName().equalsIgnoreCase(newName) &&
                warehouseRepository.existsByFarmIdAndWarehouseNameIgnoreCase(warehouse.getFarm().getId(), newName)) {
            throw new BadRequestException("Tên kho hàng đã tồn tại trong trang trại.");
        }

        warehouse.setWarehouseName(newName);
        warehouse.setAddress(request.address() != null ? request.address().trim() : null);
        warehouse.setImageUrl(request.imageUrl() != null ? request.imageUrl().trim() : null);

        Warehouse saved = warehouseRepository.save(warehouse);
        return toWarehouseResp(saved);
    }

    @Override
    public void deleteWarehouse(Integer userId, Integer warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Kho hàng ID=" + warehouseId + " không tồn tại."));
        accessGuard.requireFarmOwner(userId, warehouse.getFarm().getId());

        List<Inventory> inventories = inventoryRepository.findByWarehouseId(warehouseId);
        if (!inventories.isEmpty()) {
            inventoryRepository.deleteAll(inventories);
        }
        warehouseRepository.delete(warehouse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemResp> getInventoriesByFarm(Integer userId, Integer farmId, Integer warehouseId, String keyword) {
        accessGuard.requireFarmAccess(userId, farmId);
        List<Inventory> list;
        if (warehouseId != null) {
            list = inventoryRepository.findByWarehouseId(warehouseId);
        } else {
            list = inventoryRepository.findByFarmId(farmId);
        }

        if (keyword != null && !keyword.trim().isEmpty()) {
            String q = keyword.trim().toLowerCase();
            list = list.stream()
                    .filter(i -> (i.getMaterial() != null && i.getMaterial().getMaterialName().toLowerCase().contains(q))
                            || (i.getWarehouse() != null && i.getWarehouse().getWarehouseName().toLowerCase().contains(q)))
                    .toList();
        }

        return list.stream().map(this::toInventoryItemResp).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItemResp getInventoryDetails(Integer userId, Integer inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi tồn kho ID=" + inventoryId + " không tồn tại."));
        accessGuard.requireFarmAccess(userId, inventory.getWarehouse().getFarm().getId());
        return toInventoryItemResp(inventory);
    }

    @Override
    public InventoryItemResp updateInventory(Integer userId, Integer inventoryId, UpdateInventoryReq request) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi tồn kho ID=" + inventoryId + " không tồn tại."));
        accessGuard.requireFarmOwner(userId, inventory.getWarehouse().getFarm().getId());

        inventory.setQuantityOnHand(request.quantityOnHand());
        inventory.setReorderLevel(request.reorderLevel());
        Inventory saved = inventoryRepository.save(inventory);
        return toInventoryItemResp(saved);
    }

    @Override
    public InventoryItemResp createInventory(Integer userId, Integer farmId, CreateInventoryReq request) {
        accessGuard.requireFarmOwner(userId, farmId);

        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Kho hàng ID=" + request.warehouseId() + " không tồn tại."));
        if (!warehouse.getFarm().getId().equals(farmId)) {
            throw new BadRequestException("Kho hàng không thuộc trang trại này.");
        }

        Material material = materialRepository.findByIdAndFarmId(request.materialId(), farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Vật tư ID=" + request.materialId() + " không tồn tại trong trang trại."));

        if (inventoryRepository.findByWarehouseIdAndMaterialId(request.warehouseId(), request.materialId()).isPresent()) {
            throw new BadRequestException("Vật tư này đã tồn tại trong kho. Hãy cập nhật số lượng tồn thay vì thêm mới.");
        }

        Inventory inventory = new Inventory(
                warehouse,
                material,
                request.quantityOnHand(),
                request.reorderLevel()
        );
        Inventory saved = inventoryRepository.save(inventory);
        return toInventoryItemResp(saved);
    }

    @Override
    public void deleteInventory(Integer userId, Integer inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi tồn kho ID=" + inventoryId + " không tồn tại."));
        accessGuard.requireFarmOwner(userId, inventory.getWarehouse().getFarm().getId());
        inventoryRepository.delete(inventory);
    }

    private WarehouseResp toWarehouseResp(Warehouse w) {
        int count = w.getInventories() != null ? w.getInventories().size() : 0;
        return new WarehouseResp(
                w.getId(),
                w.getFarm().getId(),
                w.getFarm().getFarmName(),
                w.getWarehouseName(),
                w.getAddress(),
                w.getImageUrl(),
                count,
                w.getCreatedAt()
        );
    }

    private InventoryItemResp toInventoryItemResp(Inventory i) {
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
