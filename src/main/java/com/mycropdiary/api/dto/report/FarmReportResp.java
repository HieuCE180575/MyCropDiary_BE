package com.mycropdiary.api.dto.report;

import java.math.BigDecimal;
import java.util.List;

public record FarmReportResp(
        Integer farmId,
        String farmName,
        BigDecimal totalHarvestYield,
        String yieldUnit,
        int totalHarvestBatches,
        double gradeOnePercentage,
        BigDecimal totalExpenseAmount,
        int totalExpenseCount,
        BigDecimal totalPurchasesAmount,
        int totalPurchasesCount,
        BigDecimal estimatedRevenue,
        BigDecimal estimatedProfit,
        double profitMarginPercent,
        int activeSeasonCount,
        int totalBlockCount,
        List<CategoryExpenseSummary> expenseBreakdown,
        List<SeasonReportSummary> seasonSummaries
) {
    public record CategoryExpenseSummary(String category, BigDecimal amount, double percentage, String color) {}
    public record SeasonReportSummary(Integer cropSeasonId, String seasonName, String cropCategoryName, BigDecimal harvestYield, BigDecimal totalExpense, String status) {}
}

