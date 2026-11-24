package com.mycropdiary.api.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Báo cáo thống kê toàn hệ thống (System Statistics)")
public record AdminStatisticsRespDTO(
        AccountStatsRespDTO accounts,
        RegistrationStatsRespDTO registrations,
        FarmStatsRespDTO farms,
        SeasonStatsRespDTO seasons,
        AIFeedbackStatsRespDTO aiFeedback
) {
}
