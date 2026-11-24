package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.water.WaterAssessmentRespDTO;
import com.mycropdiary.api.entity.WaterAssessment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WaterAssessmentMapper {

    public WaterAssessmentRespDTO toResponse(WaterAssessment entity) {
        return WaterAssessmentRespDTO.fromEntity(entity);
    }

    public List<WaterAssessmentRespDTO> toResponseList(List<WaterAssessment> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).toList();
    }
}
