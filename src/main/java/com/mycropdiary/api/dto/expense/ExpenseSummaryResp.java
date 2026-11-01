package com.mycropdiary.api.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Báo cáo tổng hợp chi phí sản xuất (Expense and Production Cost Summary)")
public record ExpenseSummaryResp(
        Integer farmId,
        Integer cropSeasonId,
        String cropSeasonName,
        BigDecimal totalExpenseAmount,
        Integer totalExpenseCount,
        List<ExpenseCategorySummary> byCategory,
        List<ExpenseSeasonSummary> bySeason
) {
    public record ExpenseCategorySummary(
            String category,
            BigDecimal totalAmount,
            Integer count,
            BigDecimal percentage
    ) {}

    public record ExpenseSeasonSummary(
            Integer seasonId,
            String seasonName,
            BigDecimal totalAmount,
            Integer count
    ) {}
}

