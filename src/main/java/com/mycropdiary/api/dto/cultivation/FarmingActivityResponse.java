package com.mycropdiary.api.dto.cultivation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO phản hồi chi tiết thông tin nhật ký canh tác.
 */
public record FarmingActivityResponse(
        Long farmingActivityId,
        Long cropSeasonId,
        String seasonCode,
        String seasonName,
        Long farmId,
        String farmName,
        Long plotId,
        String plotCode,
        String plotName,
        Long supervisedByMemberId,
        String supervisorFullName,
        String activityType,
        String activityName,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String description,
        String resultNotes,
        String weatherNotes,
        Long farmTaskId,
        List<ActivityWorkerResponse> workers,
        LocalDateTime createdAt
) {}
