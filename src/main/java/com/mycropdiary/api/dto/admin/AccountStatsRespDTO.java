package com.mycropdiary.api.dto.admin;

public record AccountStatsRespDTO(
        long totalUsers,
        long activeUsers,
        long lockedUsers,
        long ownerCount,
        long staffCount
) {}
