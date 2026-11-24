package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.admin.*;
import org.springframework.stereotype.Component;

@Component
public class AdminStatisticsMapper {

    public AdminStatisticsRespDTO toResponse(
            AccountStatsRespDTO accounts,
            RegistrationStatsRespDTO registrations,
            FarmStatsRespDTO farms,
            SeasonStatsRespDTO seasons,
            AIFeedbackStatsRespDTO aiFeedback) {
        return new AdminStatisticsRespDTO(accounts, registrations, farms, seasons, aiFeedback);
    }
}
