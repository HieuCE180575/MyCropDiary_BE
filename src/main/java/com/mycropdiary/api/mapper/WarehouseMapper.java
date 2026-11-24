package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.warehouse.WarehouseRespDTO;
import com.mycropdiary.api.entity.cultivation.Warehouse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lớp chuyển đổi dữ liệu (Mapper) giữa Entity Warehouse và DTO.
 */
@Component
public class WarehouseMapper {

    public WarehouseRespDTO toWarehouseResp(Warehouse w) {
        if (w == null) {
            return null;
        }
        int count = w.getInventories() != null ? w.getInventories().size() : 0;
        return new WarehouseRespDTO(
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

    public WarehouseRespDTO toResponse(Warehouse w) {
        return toWarehouseResp(w);
    }

    public List<WarehouseRespDTO> toResponseList(List<Warehouse> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toWarehouseResp).toList();
    }
}
