package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.soil.SoilAssessmentRespDTO;
import com.mycropdiary.api.entity.SoilAssessment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SoilAssessmentMapper {

    public SoilAssessmentRespDTO toResponse(SoilAssessment entity) {
        return SoilAssessmentRespDTO.fromEntity(entity);
    }

    public List<SoilAssessmentRespDTO> toResponseList(List<SoilAssessment> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).toList();
    }
}
