package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.worker.CreateWorkerReqDTO;
import com.mycropdiary.api.dto.worker.UpdateWorkerReqDTO;
import com.mycropdiary.api.dto.worker.FarmWorkerRespDTO;

import java.util.List;

public interface FarmWorkerService {
    List<FarmWorkerRespDTO> getWorkersByFarm(Integer userId, Integer farmId, String status, String keyword);
    FarmWorkerRespDTO getWorkerById(Integer userId, Integer farmId, Integer workerId);
    FarmWorkerRespDTO createWorker(Integer userId, Integer farmId, CreateWorkerReqDTO req);
    FarmWorkerRespDTO updateWorker(Integer userId, Integer farmId, Integer workerId, UpdateWorkerReqDTO req);
    void deleteWorker(Integer userId, Integer farmId, Integer workerId);
}

