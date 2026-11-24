package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.vietgap.VietGapCriterionRespDTO;
import com.mycropdiary.api.entity.cultivation.AssessmentCriterion;
import org.springframework.stereotype.Component;

@Component
public class VietGapCriterionMapper {

    public VietGapCriterionRespDTO toVietGapCriterionResponse(AssessmentCriterion c, String baseDesc, String method) {
        if (c == null) {
            return null;
        }
        return new VietGapCriterionRespDTO(
                c.getId(),
                c.getFarm().getId(),
                c.getFarm().getFarmName(),
                c.getCriterionName(),
                baseDesc,
                c.getMaxScore(),
                method,
                "TCVN 11892-1:2017",
                c.getIsActive()
        );
    }
}
