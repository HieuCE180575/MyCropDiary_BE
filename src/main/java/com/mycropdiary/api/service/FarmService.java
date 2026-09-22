package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.farm.FarmSummaryResponse;

import java.util.List;

public interface FarmService {
    List<FarmSummaryResponse> findAllAccessibleFarms();
}
