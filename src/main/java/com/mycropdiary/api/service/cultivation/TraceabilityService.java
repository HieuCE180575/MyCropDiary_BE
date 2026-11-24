package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.cultivation.LotTraceabilityRespDTO;

public interface TraceabilityService {
    LotTraceabilityRespDTO getLotTraceability(Integer userId, Integer lotId);
}
