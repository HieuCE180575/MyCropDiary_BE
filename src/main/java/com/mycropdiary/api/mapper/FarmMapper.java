package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.farm.FarmSummaryResponse;
import com.mycropdiary.api.entity.Farm;
import org.springframework.stereotype.Component;

@Component
public class FarmMapper {
    public FarmSummaryResponse toSummary(Farm farm) {
        return new FarmSummaryResponse(farm.getId(), farm.getFarmCode(), farm.getFarmName(), farm.getStatus());
    }
}
