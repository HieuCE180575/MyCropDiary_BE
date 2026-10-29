package com.mycropdiary.api.dto.cultivation;

import java.time.LocalDateTime;

/**
 * DTO phản hồi tóm tắt danh sách lịch sử nhật ký canh tác.
 */
public record FarmingActivitySummaryResp(
        Integer farmingActivityId,
        Integer cropSeasonId,
        Integer blockId,
        String blockName,
        String activityType,
        String activityName,
        String description,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer supervisedByMemberId,
        String supervisorFullName,
        int workerCount,
        LocalDateTime createdAt
) {
    public FarmingActivitySummaryResp(
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
    ) {
        this(farmingActivityId, cropSeasonId, null, null, activityType, activityName, null,
                startedAt, endedAt, supervisedByMemberId, supervisorFullName, workerCount, createdAt);
    }
}
