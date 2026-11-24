package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.*;
import org.springframework.data.domain.Pageable;

public interface FarmTaskService {
    PageResp<FarmTaskRespDTO> getFarmTasks(
            Integer userId,
            Integer farmId,
            Integer cropSeasonId,
            Integer zoneId,
            String status,
            String priority,
            String keyword,
            Pageable pageable
    );

    FarmTaskRespDTO getTaskById(Integer userId, Integer taskId);

    FarmTaskRespDTO createTask(Integer userId, Integer farmId, CreateFarmTaskReqDTO request);

    FarmTaskRespDTO updateTask(Integer userId, Integer taskId, UpdateFarmTaskReqDTO request);

    FarmTaskRespDTO updateTaskStatus(Integer userId, Integer taskId, UpdateFarmTaskStatusReqDTO request);

    void deleteTask(Integer userId, Integer taskId);
}

