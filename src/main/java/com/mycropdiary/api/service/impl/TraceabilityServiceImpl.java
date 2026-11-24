package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.cultivation.LotTraceabilityRespDTO;
import com.mycropdiary.api.entity.cultivation.HarvestRecord;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.TraceabilityMapper;
import com.mycropdiary.api.repository.cultivation.TraceabilityRepository;
import com.mycropdiary.api.service.cultivation.TraceabilityService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TraceabilityServiceImpl implements TraceabilityService {

    private final TraceabilityRepository traceabilityRepository;
    private final FarmAccessGuard accessGuard;
    private final TraceabilityMapper traceabilityMapper;

    @Autowired
    public TraceabilityServiceImpl(
            TraceabilityRepository traceabilityRepository,
            FarmAccessGuard accessGuard,
            TraceabilityMapper traceabilityMapper) {
        this.traceabilityRepository = traceabilityRepository;
        this.accessGuard = accessGuard;
        this.traceabilityMapper = traceabilityMapper != null ? traceabilityMapper : new TraceabilityMapper();
    }

    @Override
    public LotTraceabilityRespDTO getLotTraceability(Integer userId, Integer lotId) {
        HarvestRecord record = traceabilityRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô thu hoạch ID=" + lotId));
        if (record.getFarm() != null) {
            accessGuard.requireFarmAccess(userId, record.getFarm().getId());
        }
        return traceabilityMapper.toResponse(record);
    }
}
