package com.mycropdiary.api.dto.cultivation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO yêu cầu tạo mới nhật ký canh tác (UC-24).
 */
public record CreateFarmingActivityRequest(
        @NotNull(message = "Crop season ID is required")
        Long cropSeasonId,

        @NotBlank(message = "Activity type is required")
        @Size(max = 40, message = "Activity type must not exceed 40 characters")
        String activityType,

        @NotBlank(message = "Activity name is required")
        @Size(max = 200, message = "Activity name must not exceed 200 characters")
        String activityName,

        @NotNull(message = "Started at time is required")
        LocalDateTime startedAt,

        LocalDateTime endedAt,

        String description,

        String resultNotes,

        @Size(max = 1000, message = "Weather notes must not exceed 1000 characters")
        String weatherNotes,

        Long farmTaskId,

        @Valid
        List<ActivityWorkerRequest> workers
) {}
