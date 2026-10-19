package com.mycropdiary.api.dto.cultivation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO phản hồi chi tiết thông tin nhật ký canh tác.
 */
public record FarmingActivityResp(
        Integer farmingActivityId,
        Integer cropSeasonId,
        String seasonCode,
        String seasonName,
        Integer farmId,
        String farmName,
        Integer blockId,
        String blockName,
        Integer supervisedByMemberId,
        String supervisorFullName,
        String activityType,
        String activityName,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String description,
        String resultNotes,
        String weatherNotes,
        Integer farmTaskId,
        List<ActivityWorkerResp> workers,
        LocalDateTime createdAt
) {
    public FarmingActivityResp(
            Integer farmingActivityId, Integer cropSeasonId, String seasonCode, String seasonName,
            Integer farmId, String farmName, Integer plotId, String plotCode, String plotName,
            Integer supervisedByMemberId, String supervisorFullName, String activityType, String activityName,
            LocalDateTime startedAt, LocalDateTime endedAt, String description, String resultNotes,
            String weatherNotes, Integer farmTaskId, List<ActivityWorkerResp> workers, LocalDateTime createdAt
    ) {
        this(farmingActivityId, cropSeasonId, seasonCode, seasonName, farmId, farmName,
                plotId, plotName != null ? plotName : plotCode, supervisedByMemberId, supervisorFullName,
                activityType, activityName, startedAt, endedAt, description, resultNotes, weatherNotes,
                farmTaskId, workers, createdAt);
    }

    public Integer plotId() { return blockId; }
    public String plotCode() { return blockName; }
    public String plotName() { return blockName; }
}
