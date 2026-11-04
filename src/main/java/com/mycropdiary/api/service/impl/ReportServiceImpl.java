package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.report.CropSeasonReportResp;
import com.mycropdiary.api.dto.report.FarmReportResp;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.Expense;
import com.mycropdiary.api.entity.cultivation.HarvestRecord;
import com.mycropdiary.api.entity.cultivation.InputPurchase;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.*;
import com.mycropdiary.api.service.cultivation.ReportService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final FarmRepository farmRepository;
    private final CropSeasonRepository cropSeasonRepository;
    private final BlockRepository blockRepository;
    private final HarvestRecordRepository harvestRecordRepository;
    private final ExpenseRepository expenseRepository;
    private final InputPurchaseRepository inputPurchaseRepository;
    private final FarmingActivityRepository farmingActivityRepository;
    private final FarmAccessGuard accessGuard;

    public ReportServiceImpl(FarmRepository farmRepository,
                             CropSeasonRepository cropSeasonRepository,
                             BlockRepository blockRepository,
                             HarvestRecordRepository harvestRecordRepository,
                             ExpenseRepository expenseRepository,
                             InputPurchaseRepository inputPurchaseRepository,
                             FarmingActivityRepository farmingActivityRepository,
                             FarmAccessGuard accessGuard) {
        this.farmRepository = farmRepository;
        this.cropSeasonRepository = cropSeasonRepository;
        this.blockRepository = blockRepository;
        this.harvestRecordRepository = harvestRecordRepository;
        this.expenseRepository = expenseRepository;
        this.inputPurchaseRepository = inputPurchaseRepository;
        this.farmingActivityRepository = farmingActivityRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    public FarmReportResp generateFarmReport(Integer userId, Integer farmId) {
        accessGuard.requireFarmAccess(userId, farmId);
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Trang trại không tồn tại."));

        // 1. Harvests
        List<HarvestRecord> harvests = harvestRecordRepository.findByFarmId(farmId);
        BigDecimal totalYield = BigDecimal.ZERO;
        String yieldUnit = "kg";
        int gradeOneCount = 0;
        for (HarvestRecord h : harvests) {
            if (h.getQuantity() != null) {
                totalYield = totalYield.add(h.getQuantity());
            }
            if (h.getUnit() != null) {
                yieldUnit = h.getUnit();
            }
            if (h.getQualityGrade() != null && (h.getQualityGrade().contains("Loại 1") || h.getQualityGrade().equalsIgnoreCase("A") || h.getQualityGrade().contains("Xuất khẩu"))) {
                gradeOneCount++;
            }
        }
        double gradeOnePercentage = harvests.isEmpty() ? 0.0 : Math.round((gradeOneCount * 100.0 / harvests.size()) * 10.0) / 10.0;

        // 2. Expenses
        List<Expense> expenses = expenseRepository.findExpensesForSummary(farmId, null, null, null);
        BigDecimal totalExpense = BigDecimal.ZERO;
        Map<String, BigDecimal> expenseByCategoryMap = new HashMap<>();
        for (Expense e : expenses) {
            if (e.getAmount() != null) {
                totalExpense = totalExpense.add(e.getAmount());
                String cat = e.getExpenseCategory() != null ? e.getExpenseCategory() : "Khác";
                expenseByCategoryMap.put(cat, expenseByCategoryMap.getOrDefault(cat, BigDecimal.ZERO).add(e.getAmount()));
            }
        }

        List<FarmReportResp.CategoryExpenseSummary> breakdown = new ArrayList<>();
        String[] colors = {"#059669", "#10b981", "#0284c7", "#8b5cf6", "#f59e0b", "#ef4444", "#64748b"};
        int colorIdx = 0;
        for (Map.Entry<String, BigDecimal> entry : expenseByCategoryMap.entrySet()) {
            double pct = totalExpense.compareTo(BigDecimal.ZERO) > 0
                    ? entry.getValue().multiply(BigDecimal.valueOf(100)).divide(totalExpense, 1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            breakdown.add(new FarmReportResp.CategoryExpenseSummary(entry.getKey(), entry.getValue(), pct, colors[colorIdx % colors.length]));
            colorIdx++;
        }
        breakdown.sort((a, b) -> b.amount().compareTo(a.amount()));

        // 3. Purchases
        List<InputPurchase> purchases = inputPurchaseRepository.findByFarmIdOrderByPurchaseDateDesc(farmId);
        BigDecimal totalPurchases = BigDecimal.ZERO;
        for (InputPurchase p : purchases) {
            if (p.getTotalAmount() != null) {
                totalPurchases = totalPurchases.add(p.getTotalAmount());
            }
        }

        // 4. Financial projections (estimated wholesale price: 45,000 VND / kg)
        BigDecimal estimatedRevenue = totalYield.multiply(new BigDecimal("45000"));
        BigDecimal estimatedProfit = estimatedRevenue.subtract(totalExpense);
        double profitMargin = estimatedRevenue.compareTo(BigDecimal.ZERO) > 0
                ? estimatedProfit.multiply(BigDecimal.valueOf(100)).divide(estimatedRevenue, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        // 5. Seasons & Blocks
        List<CropSeason> seasons = cropSeasonRepository.findByFarmIdOrderByStartDateDesc(farmId);
        List<Block> blocks = blockRepository.findByZoneFarmId(farmId);
        int activeSeasonCount = (int) seasons.stream().filter(s -> "active".equalsIgnoreCase(s.getSeasonStatus()) || "in_progress".equalsIgnoreCase(s.getSeasonStatus())).count();

        List<FarmReportResp.SeasonReportSummary> seasonSummaries = new ArrayList<>();
        for (CropSeason s : seasons) {
            List<HarvestRecord> seasonHarvests = harvestRecordRepository.findByCropSeasonId(s.getId());
            BigDecimal sYield = seasonHarvests.stream().map(h -> h.getQuantity() != null ? h.getQuantity() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
            List<Expense> seasonExpenses = expenseRepository.findExpensesForSummary(farmId, s.getId(), null, null);
            BigDecimal sExp = seasonExpenses.stream().map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

            String catName = s.getCropCategory() != null ? s.getCropCategory().getCategoryName() : null;
            seasonSummaries.add(new FarmReportResp.SeasonReportSummary(s.getId(), s.getSeasonName(), catName, sYield, sExp, s.getSeasonStatus()));
        }

        return new FarmReportResp(
                farm.getId(),
                farm.getFarmName(),
                totalYield,
                yieldUnit,
                harvests.size(),
                gradeOnePercentage,
                totalExpense,
                expenses.size(),
                totalPurchases,
                purchases.size(),
                estimatedRevenue,
                estimatedProfit,
                profitMargin,
                activeSeasonCount,
                blocks.size(),
                breakdown,
                seasonSummaries
        );
    }

    @Override
    public CropSeasonReportResp getCropSeasonReport(Integer userId, Integer cropSeasonId) {
        CropSeason season = cropSeasonRepository.findById(cropSeasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại."));
        accessGuard.requireFarmAccess(userId, season.getFarm().getId());

        List<HarvestRecord> harvests = harvestRecordRepository.findByCropSeasonId(cropSeasonId);
        BigDecimal totalYield = harvests.stream().map(h -> h.getQuantity() != null ? h.getQuantity() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        String unit = harvests.isEmpty() ? "kg" : harvests.get(0).getUnit();

        List<Expense> expenses = expenseRepository.findExpensesForSummary(season.getFarm().getId(), cropSeasonId, null, null);
        BigDecimal totalExpenses = expenses.stream().map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> expenseByCategoryMap = new HashMap<>();
        for (Expense e : expenses) {
            if (e.getAmount() != null) {
                String cat = e.getExpenseCategory() != null ? e.getExpenseCategory() : "Khác";
                expenseByCategoryMap.put(cat, expenseByCategoryMap.getOrDefault(cat, BigDecimal.ZERO).add(e.getAmount()));
            }
        }
        List<FarmReportResp.CategoryExpenseSummary> breakdown = new ArrayList<>();
        String[] colors = {"#059669", "#10b981", "#0284c7", "#8b5cf6", "#f59e0b", "#ef4444"};
        int idx = 0;
        for (Map.Entry<String, BigDecimal> entry : expenseByCategoryMap.entrySet()) {
            double pct = totalExpenses.compareTo(BigDecimal.ZERO) > 0
                    ? entry.getValue().multiply(BigDecimal.valueOf(100)).divide(totalExpenses, 1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            breakdown.add(new FarmReportResp.CategoryExpenseSummary(entry.getKey(), entry.getValue(), pct, colors[idx % colors.length]));
            idx++;
        }

        long activityCount = farmingActivityRepository.countByCropSeasonId(cropSeasonId);
        String catName = season.getCropCategory() != null ? season.getCropCategory().getCategoryName() : null;

        return new CropSeasonReportResp(
                season.getId(),
                season.getSeasonName(),
                season.getFarm().getId(),
                season.getFarm().getFarmName(),
                catName,
                season.getStartDate(),
                season.getEndDate(),
                season.getSeasonStatus(),
                totalYield,
                unit,
                harvests.size(),
                totalExpenses,
                (int) activityCount,
                0,
                breakdown
        );
    }
}

