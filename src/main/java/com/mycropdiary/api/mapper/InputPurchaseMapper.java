package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.procurement.InputPurchaseRespDTO;
import com.mycropdiary.api.dto.procurement.PurchaseItemRespDTO;
import com.mycropdiary.api.entity.cultivation.InputPurchase;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InputPurchaseMapper {

    public InputPurchaseRespDTO toResponse(InputPurchase entity, List<PurchaseItemRespDTO> items) {
        return InputPurchaseRespDTO.fromEntity(entity, items);
    }
}
