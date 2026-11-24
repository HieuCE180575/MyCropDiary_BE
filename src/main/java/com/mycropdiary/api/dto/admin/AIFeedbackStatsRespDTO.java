package com.mycropdiary.api.dto.admin;

public record AIFeedbackStatsRespDTO(
        long totalFeedbacks,
        long openFeedbacks,
        long resolvedFeedbacks,
        double averageRating
) {}
