package com.mycropdiary.api.dto.cultivation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO yêu cầu tạo mới nhật ký canh tác (UC-24).
 */
public record CreateFarmingActivityRequest(
        @NotNull(message = "Crop season ID is required")
        UUID cropSeasonId,

        @NotBlank(message = "Activity type is required")
        @Size(max = 60, message = "Activity type must not exceed 60 characters")
        String activityType,

        @NotBlank(message = "Activity name is required")
        @Size(max = 160, message = "Activity name must not exceed 160 characters")
        String activityName,

        @NotNull(message = "Started at time is required")
        LocalDateTime startedAt,

        LocalDateTime endedAt,

        String description,

        String resultNotes,

        @Size(max = 1000, message = "Weather notes must not exceed 1000 characters")
        String weatherNotes,

        UUID farmTaskId,

        UUID zoneId,

        UUID blockId,

        @Valid
        List<ActivityWorkerRequest> workers
) {
    public CreateFarmingActivityRequest(
            UUID cropSeasonId,
            String activityType,
            String activityName,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            String description,
            String resultNotes,
            String weatherNotes,
            UUID farmTaskId,
            List<ActivityWorkerRequest> workers
    ) {
        this(cropSeasonId, activityType, activityName, startedAt, endedAt, description, resultNotes, weatherNotes, farmTaskId, null, null, workers);
    }
}
