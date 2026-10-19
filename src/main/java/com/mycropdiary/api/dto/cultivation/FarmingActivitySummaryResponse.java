package com.mycropdiary.api.dto.cultivation;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO phản hồi tóm tắt danh sách lịch sử nhật ký canh tác.
 */
public record FarmingActivitySummaryResponse(
        UUID farmingActivityId,
        UUID cropSeasonId,
        String activityType,
        String activityName,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        UUID supervisedByMemberId,
        String supervisorFullName,
        int workerCount,
        LocalDateTime createdAt
) {}
