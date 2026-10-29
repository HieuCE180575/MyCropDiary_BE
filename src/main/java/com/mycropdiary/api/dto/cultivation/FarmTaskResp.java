package com.mycropdiary.api.dto.cultivation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record FarmTaskResp(
        Integer taskId,
        Integer farmId,
        String farmName,
        Integer cropSeasonId,
        String seasonName,
        Integer zoneId,
        String zoneName,
        Integer blockId,
        String blockName,
        String taskName,
        String description,
        String taskType,
        String priority,
        String status,
        LocalDateTime startsAt,
        LocalDateTime dueAt,
        Integer createdByUserId,
        String createdByFullName,
        List<TaskWorkerResp> assignees,
        Instant createdAt
) {}
