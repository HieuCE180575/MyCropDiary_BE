package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.vietgap.AssessmentRespDTO;
import com.mycropdiary.api.dto.vietgap.InternalAssessmentItemRespDTO;
import com.mycropdiary.api.entity.cultivation.InternalAssessment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InternalAssessmentMapper {

    public AssessmentRespDTO toResponse(InternalAssessment entity, List<InternalAssessmentItemRespDTO> items) {
        return AssessmentRespDTO.fromEntity(entity, items);
    }
}
