package com.mycropdiary.api.dto.cultivation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO phản hồi chi tiết thông tin nhật ký canh tác.
 */
public record FarmingActivityResponse(
        UUID farmingActivityId,
        UUID cropSeasonId,
        String seasonCode,
        String seasonName,
        UUID farmId,
        String farmName,
        UUID blockId,
        String blockName,
        UUID supervisedByMemberId,
        String supervisorFullName,
        String activityType,
        String activityName,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String description,
        String resultNotes,
        String weatherNotes,
        UUID farmTaskId,
        List<ActivityWorkerResponse> workers,
        LocalDateTime createdAt
) {
    public FarmingActivityResponse(
            UUID farmingActivityId, UUID cropSeasonId, String seasonCode, String seasonName,
            UUID farmId, String farmName, UUID plotId, String plotCode, String plotName,
            UUID supervisedByMemberId, String supervisorFullName, String activityType, String activityName,
            LocalDateTime startedAt, LocalDateTime endedAt, String description, String resultNotes,
            String weatherNotes, UUID farmTaskId, List<ActivityWorkerResponse> workers, LocalDateTime createdAt
    ) {
        this(farmingActivityId, cropSeasonId, seasonCode, seasonName, farmId, farmName,
                plotId, plotName != null ? plotName : plotCode, supervisedByMemberId, supervisorFullName,
                activityType, activityName, startedAt, endedAt, description, resultNotes, weatherNotes,
                farmTaskId, workers, createdAt);
    }

    public UUID plotId() { return blockId; }
    public String plotCode() { return blockName; }
    public String plotName() { return blockName; }
}
