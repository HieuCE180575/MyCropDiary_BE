package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.report.CropSeasonReportRespDTO;
import com.mycropdiary.api.dto.report.FarmReportRespDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class ProductionReportMapper {

    public FarmReportRespDTO toFarmReportResponse(
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
            List<FarmReportRespDTO.CategoryExpenseSummary> expenseBreakdown,
            List<FarmReportRespDTO.SeasonReportSummary> seasonSummaries) {
        return new FarmReportRespDTO(
                farmId, farmName, totalHarvestYield, yieldUnit, totalHarvestBatches, gradeOnePercentage,
                totalExpenseAmount, totalExpenseCount, totalPurchasesAmount, totalPurchasesCount,
                estimatedRevenue, estimatedProfit, profitMarginPercent, activeSeasonCount, totalBlockCount,
                expenseBreakdown, seasonSummaries
        );
    }

    public CropSeasonReportRespDTO toSeasonReportResponse(
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
            List<FarmReportRespDTO.CategoryExpenseSummary> expenseBreakdown) {
        return new CropSeasonReportRespDTO(
                cropSeasonId, seasonName, farmId, farmName, cropCategoryName, startDate, endDate,
                seasonStatus, totalYield, unit, harvestCount, totalExpenses, activityCount,
                materialUsageCount, expenseBreakdown
        );
    }
}
