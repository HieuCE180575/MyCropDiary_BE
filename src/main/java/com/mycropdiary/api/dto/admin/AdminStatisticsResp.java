package com.mycropdiary.api.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Báo cáo thống kê toàn hệ thống (System Statistics)")
public record AdminStatisticsResp(
        AccountStatistics accounts,
        RegistrationStatistics registrations,
        FarmStatistics farms,
        SeasonStatistics seasons,
        AIFeedbackStatistics aiFeedback
) {
    public record AccountStatistics(
            long totalUsers,
            long activeUsers,
            long lockedUsers,
            long ownerCount,
            long staffCount
    ) {}

    public record RegistrationStatistics(
            long totalRegistrations,
            long pendingRegistrations,
            long approvedRegistrations,
            long rejectedRegistrations
    ) {}

    public record FarmStatistics(
            long totalFarms,
            long activeFarms
    ) {}

    public record SeasonStatistics(
            long totalSeasons,
            long activeSeasons,
            long completedSeasons
    ) {}

    public record AIFeedbackStatistics(
            long totalFeedbacks,
            long openFeedbacks,
            long resolvedFeedbacks,
            double averageRating
    ) {}
}

