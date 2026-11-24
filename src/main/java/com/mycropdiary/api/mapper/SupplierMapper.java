package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.procurement.SupplierRespDTO;
import com.mycropdiary.api.entity.cultivation.Supplier;
import org.springframework.stereotype.Component;

@Component
public class SupplierMapper {

    public SupplierRespDTO toResponse(Supplier entity, Long purchaseCount) {
        return SupplierRespDTO.fromEntity(entity, purchaseCount);
    }
}
