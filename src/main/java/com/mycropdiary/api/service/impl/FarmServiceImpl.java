package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.farm.FarmSummaryResponse;
import com.mycropdiary.api.mapper.FarmMapper;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.service.FarmService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FarmServiceImpl implements FarmService {
    private final FarmRepository farmRepository;
    private final FarmMapper farmMapper;

    public FarmServiceImpl(FarmRepository farmRepository, FarmMapper farmMapper) {
        this.farmRepository = farmRepository;
        this.farmMapper = farmMapper;
    }

    @Override
    public List<FarmSummaryResponse> findAllAccessibleFarms() {
        // TODO: filter by authenticated user and farm membership.
        return farmRepository.findAll().stream().map(farmMapper::toSummary).toList();
    }
}
