package com.mycropdiary.api.dto.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CropSeasonReportResp(
        Integer cropSeasonId,
        String seasonName,
        Integer farmId,
        String farmName,
        String cropCategoryName,
        LocalDate startDate,
        LocalDate endDate,
        String seasonStatus,
        BigDecimal totalYield,
        String unit,
        int harvestCount,
        BigDecimal totalExpenses,
        int activityCount,
        int materialUsageCount,
        List<FarmReportResp.CategoryExpenseSummary> expenseBreakdown
) {
}

