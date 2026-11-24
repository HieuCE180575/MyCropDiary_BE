package com.mycropdiary.api.dto.admin;

public record SeasonStatsRespDTO(
        long totalSeasons,
        long activeSeasons,
        long completedSeasons
) {}
