package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.worker.FarmWorkerReq;
import com.mycropdiary.api.dto.worker.FarmWorkerResp;

import java.util.List;

public interface FarmWorkerService {
    List<FarmWorkerResp> getWorkersByFarm(Integer userId, Integer farmId, String status, String keyword);
    FarmWorkerResp getWorkerById(Integer userId, Integer farmId, Integer workerId);
    FarmWorkerResp createWorker(Integer userId, Integer farmId, FarmWorkerReq req);
    FarmWorkerResp updateWorker(Integer userId, Integer farmId, Integer workerId, FarmWorkerReq req);
    void deleteWorker(Integer userId, Integer farmId, Integer workerId);
}

