package com.mycropdiary.api.dto.cultivation;

import java.time.LocalDateTime;

/**
 * DTO phản hồi tóm tắt danh sách lịch sử nhật ký canh tác.
 */
public record FarmingActivitySummaryResp(
        Integer farmingActivityId,
        Integer cropSeasonId,
        String activityType,
        String activityName,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer supervisedByMemberId,
        String supervisorFullName,
        int workerCount,
        LocalDateTime createdAt
) {}
