package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.*;
import org.springframework.data.domain.Pageable;

public interface FarmTaskService {
    PageResp<FarmTaskResp> getFarmTasks(
            Integer userId,
            Integer farmId,
            Integer cropSeasonId,
            Integer zoneId,
            String status,
            String priority,
            String keyword,
            Pageable pageable
    );

    FarmTaskResp getTaskById(Integer userId, Integer taskId);

    FarmTaskResp createTask(Integer userId, Integer farmId, CreateFarmTaskReq request);

    FarmTaskResp updateTask(Integer userId, Integer taskId, UpdateFarmTaskReq request);

    FarmTaskResp updateTaskStatus(Integer userId, Integer taskId, UpdateFarmTaskStatusReq request);

    void deleteTask(Integer userId, Integer taskId);
}
