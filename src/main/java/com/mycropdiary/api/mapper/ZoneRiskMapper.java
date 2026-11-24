package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.risk.ZoneRiskRespDTO;
import com.mycropdiary.api.entity.ZoneRiskAssessment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ZoneRiskMapper {

    public ZoneRiskRespDTO toResponse(ZoneRiskAssessment entity) {
        return ZoneRiskRespDTO.fromEntity(entity);
    }

    public List<ZoneRiskRespDTO> toResponseList(List<ZoneRiskAssessment> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).toList();
    }
}
