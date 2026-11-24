package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.warehouse.InventoryRespDTO;
import com.mycropdiary.api.entity.cultivation.Inventory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InventoryMapper {

    public InventoryRespDTO toResponse(Inventory i) {
        if (i == null) {
            return null;
        }
        String matType = i.getMaterial().getMaterialType();
        boolean lowStock = i.getQuantityOnHand().compareTo(i.getReorderLevel()) <= 0;
        return new InventoryRespDTO(
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

    public List<InventoryRespDTO> toResponseList(List<Inventory> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).toList();
    }
}
