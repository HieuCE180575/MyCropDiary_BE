package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.warehouse.CreateWarehouseReqDTO;
import com.mycropdiary.api.dto.warehouse.InventoryRespDTO;
import com.mycropdiary.api.dto.warehouse.UpdateWarehouseReqDTO;
import com.mycropdiary.api.dto.warehouse.WarehouseOverviewRespDTO;
import com.mycropdiary.api.dto.warehouse.WarehouseRespDTO;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.cultivation.Inventory;
import com.mycropdiary.api.entity.cultivation.Warehouse;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.InventoryMapper;
import com.mycropdiary.api.mapper.WarehouseMapper;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.InventoryRepository;
import com.mycropdiary.api.repository.cultivation.MaterialRepository;
import com.mycropdiary.api.repository.cultivation.WarehouseRepository;
import com.mycropdiary.api.service.cultivation.WarehouseService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final MaterialRepository materialRepository;
    private final FarmRepository farmRepository;
    private final FarmAccessGuard accessGuard;
    private final WarehouseMapper warehouseMapper;
    private final InventoryMapper inventoryMapper;

    @Autowired
    public WarehouseServiceImpl(
            WarehouseRepository warehouseRepository,
            InventoryRepository inventoryRepository,
            MaterialRepository materialRepository,
            FarmRepository farmRepository,
            FarmAccessGuard accessGuard,
            WarehouseMapper warehouseMapper,
            InventoryMapper inventoryMapper) {
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
        this.materialRepository = materialRepository;
        this.farmRepository = farmRepository;
        this.accessGuard = accessGuard;
        this.warehouseMapper = warehouseMapper != null ? warehouseMapper : new WarehouseMapper();
        this.inventoryMapper = inventoryMapper != null ? inventoryMapper : new InventoryMapper();
    }

    public WarehouseServiceImpl(
            WarehouseRepository warehouseRepository,
            InventoryRepository inventoryRepository,
            MaterialRepository materialRepository,
            FarmRepository farmRepository,
            FarmAccessGuard accessGuard) {
        this(warehouseRepository, inventoryRepository, materialRepository, farmRepository, accessGuard, new WarehouseMapper(), new InventoryMapper());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseRespDTO> getWarehousesByFarm(Integer userId, Integer farmId) {
        accessGuard.requireFarmAccess(userId, farmId);
        List<Warehouse> list = warehouseRepository.findByFarmIdOrderByWarehouseNameAsc(farmId);
        return warehouseMapper.toResponseList(list);
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseRespDTO getWarehouseDetails(Integer userId, Integer warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Kho hàng ID=" + warehouseId + " không tồn tại."));
        accessGuard.requireFarmAccess(userId, warehouse.getFarm().getId());
        return warehouseMapper.toResponse(warehouse);
    }

    @Override
    public WarehouseRespDTO createWarehouse(Integer userId, Integer farmId, CreateWarehouseReqDTO request) {
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
                request.imageUrl() != null ? request.imageUrl().trim() : null);
        warehouse.setWarehouseType(
                request.warehouseType() != null && !request.warehouseType().isBlank()
                        ? request.warehouseType().trim()
                        : "kho_chung");

        Warehouse saved = warehouseRepository.save(warehouse);
        return warehouseMapper.toResponse(saved);
    }

    @Override
    public WarehouseRespDTO updateWarehouse(Integer userId, Integer warehouseId, UpdateWarehouseReqDTO request) {
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
        if (request.warehouseType() != null && !request.warehouseType().isBlank()) {
            warehouse.setWarehouseType(request.warehouseType().trim());
        }

        Warehouse saved = warehouseRepository.save(warehouse);
        return warehouseMapper.toResponse(saved);
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
    public WarehouseOverviewRespDTO getWarehouseOverview(Integer currentUserId, Integer warehouseId) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Kho hàng ID=" + warehouseId + " không tồn tại."));

        accessGuard.requireFarmAccess(currentUserId, warehouse.getFarm().getId());

        List<Inventory> inventories = inventoryRepository.findByWarehouseId(warehouseId);

        int totalItemCount = inventories.size();
        int lowStockCount = 0;
        int outOfStockCount = 0;

        for (Inventory inventory : inventories) {
            BigDecimal quantityOnHand = inventory.getQuantityOnHand() != null ? inventory.getQuantityOnHand() : BigDecimal.ZERO;
            BigDecimal reorderLevel = inventory.getReorderLevel() != null ? inventory.getReorderLevel() : BigDecimal.ZERO;

            if (quantityOnHand.compareTo(BigDecimal.ZERO) <= 0) {
                outOfStockCount++;
            } else if (quantityOnHand.compareTo(reorderLevel) <= 0) {
                lowStockCount++;
            }
        }

        List<InventoryRespDTO> recentMaterials = inventories.stream()
                .sorted(Comparator.comparing(Inventory::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(inventoryMapper::toResponse)
                .toList();

        return new WarehouseOverviewRespDTO(
                warehouse.getId(),
                warehouse.getWarehouseName(),
                warehouse.getFarm().getFarmName(),
                warehouse.getAddress(),
                totalItemCount,
                lowStockCount,
                outOfStockCount,
                recentMaterials);
    }
}
