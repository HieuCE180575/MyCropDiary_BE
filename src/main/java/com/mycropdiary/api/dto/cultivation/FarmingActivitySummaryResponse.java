package com.mycropdiary.api.dto.cultivation;

import java.time.LocalDateTime;

/**
 * DTO phản hồi tóm tắt danh sách lịch sử nhật ký canh tác.
 */
public record FarmingActivitySummaryResponse(
        Long farmingActivityId,
        Long cropSeasonId,
        String activityType,
        String activityName,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long supervisedByMemberId,
        String supervisorFullName,
        int workerCount,
        LocalDateTime createdAt
) {}
