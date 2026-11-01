package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.expense.CreateExpenseReq;
import com.mycropdiary.api.dto.expense.ExpenseResp;
import com.mycropdiary.api.dto.expense.ExpenseSummaryResp;
import com.mycropdiary.api.dto.expense.UpdateExpenseReq;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.Expense;
import com.mycropdiary.api.entity.cultivation.FarmTask;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.cultivation.ExpenseRepository;
import com.mycropdiary.api.repository.cultivation.FarmTaskRepository;
import com.mycropdiary.api.service.cultivation.ExpenseService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final FarmRepository farmRepository;
    private final CropSeasonRepository cropSeasonRepository;
    private final FarmTaskRepository farmTaskRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final FarmAccessGuard accessGuard;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository,
                              FarmRepository farmRepository,
                              CropSeasonRepository cropSeasonRepository,
                              FarmTaskRepository farmTaskRepository,
                              FarmMemberRepository farmMemberRepository,
                              FarmAccessGuard accessGuard) {
        this.expenseRepository = expenseRepository;
        this.farmRepository = farmRepository;
        this.cropSeasonRepository = cropSeasonRepository;
        this.farmTaskRepository = farmTaskRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseResp> getExpenseList(
            Integer currentUserId,
            Integer farmId,
            Integer cropSeasonId,
            String category,
            LocalDate startDate,
            LocalDate endDate,
            String keyword
    ) {
        accessGuard.requireFarmAccess(currentUserId, farmId);

        String trimmedCategory = (category != null && !category.isBlank()) ? category.trim() : null;
        String trimmedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;

        List<Expense> list = expenseRepository.searchExpenses(farmId, cropSeasonId, trimmedCategory, startDate, endDate, trimmedKeyword);
        return list.stream().map(ExpenseResp::fromEntity).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResp getExpenseDetails(Integer currentUserId, Integer expenseId) {
        Expense expense = expenseRepository.findByIdWithDetails(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi phí ID: " + expenseId));

        accessGuard.requireFarmAccess(currentUserId, expense.getFarm().getId());
        return ExpenseResp.fromEntity(expense);
    }

    @Override
    public ExpenseResp recordExpense(Integer currentUserId, Integer farmId, CreateExpenseReq request) {
        accessGuard.requireFarmAccess(currentUserId, farmId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trang trại ID: " + farmId));

        if (request.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Số tiền chi phí không được âm");
        }

        CropSeason season = null;
        if (request.cropSeasonId() != null) {
            season = cropSeasonRepository.findById(request.cropSeasonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mùa vụ ID: " + request.cropSeasonId()));
            if (!season.getFarm().getId().equals(farmId)) {
                throw new BadRequestException("Mùa vụ không thuộc trang trại này");
            }
        }

        FarmTask task = null;
        if (request.farmTaskId() != null) {
            task = farmTaskRepository.findById(request.farmTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc ID: " + request.farmTaskId()));
            if (!task.getFarm().getId().equals(farmId)) {
                throw new BadRequestException("Công việc không thuộc trang trại này");
            }
        }

        FarmMember recordedBy = farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, currentUserId, "active")
                .orElse(null);

        Expense expense = new Expense(
                farm,
                season,
                task,
                request.expenseCategory().trim(),
                request.description() != null ? request.description().trim() : null,
                request.amount(),
                request.expenseDate() != null ? request.expenseDate() : LocalDate.now(),
                recordedBy,
                request.receiptImageUrl() != null ? request.receiptImageUrl().trim() : null
        );

        Expense saved = expenseRepository.save(expense);
        return ExpenseResp.fromEntity(saved);
    }

    @Override
    public ExpenseResp updateExpense(Integer currentUserId, Integer expenseId, UpdateExpenseReq request) {
        Expense expense = expenseRepository.findByIdWithDetails(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi phí ID: " + expenseId));

        Integer farmId = expense.getFarm().getId();
        accessGuard.requireFarmAccess(currentUserId, farmId);

        if (request.amount() != null) {
            if (request.amount().compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Số tiền chi phí không được âm");
            }
            expense.setAmount(request.amount());
        }

        if (request.expenseCategory() != null && !request.expenseCategory().isBlank()) {
            expense.setExpenseCategory(request.expenseCategory().trim());
        }

        if (request.expenseDate() != null) {
            expense.setExpenseDate(request.expenseDate());
        }

        if (request.cropSeasonId() != null) {
            CropSeason season = cropSeasonRepository.findById(request.cropSeasonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mùa vụ ID: " + request.cropSeasonId()));
            if (!season.getFarm().getId().equals(farmId)) {
                throw new BadRequestException("Mùa vụ không thuộc trang trại này");
            }
            expense.setCropSeason(season);
        }

        if (request.farmTaskId() != null) {
            FarmTask task = farmTaskRepository.findById(request.farmTaskId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công việc ID: " + request.farmTaskId()));
            if (!task.getFarm().getId().equals(farmId)) {
                throw new BadRequestException("Công việc không thuộc trang trại này");
            }
            expense.setFarmTask(task);
        }

        if (request.description() != null) {
            expense.setDescription(request.description().trim());
        }

        if (request.receiptImageUrl() != null) {
            expense.setReceiptImageUrl(request.receiptImageUrl().trim());
        }

        Expense updated = expenseRepository.save(expense);
        return ExpenseResp.fromEntity(updated);
    }

    @Override
    public void deleteExpense(Integer currentUserId, Integer expenseId) {
        Expense expense = expenseRepository.findByIdWithDetails(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chi phí ID: " + expenseId));

        Integer farmId = expense.getFarm().getId();

        // Business Rule: Delete Eligible Expense - Chỉ Farm Owner, Farm Manager hoặc người tạo chi phí mới có quyền xóa
        FarmMember currentMember = farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, currentUserId, "active").orElse(null);
        boolean isOwnerOrAdmin = accessGuard.isSystemAdmin(currentUserId)
                || (currentMember != null && ("owner".equalsIgnoreCase(currentMember.getMemberRole()) || "manager".equalsIgnoreCase(currentMember.getMemberRole())));
        boolean isCreator = expense.getRecordedBy() != null && currentMember != null && expense.getRecordedBy().getId().equals(currentMember.getId());

        if (!isOwnerOrAdmin && !isCreator) {
            throw new BadRequestException("Bạn không có quyền xóa khoản chi phí này (yêu cầu Farm Owner, Manager hoặc người ghi nhận).");
        }

        expenseRepository.delete(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseSummaryResp getExpenseSummary(
            Integer currentUserId,
            Integer farmId,
            Integer cropSeasonId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        accessGuard.requireFarmAccess(currentUserId, farmId);

        List<Expense> expenses = expenseRepository.findExpensesForSummary(farmId, cropSeasonId, startDate, endDate);

        BigDecimal grandTotal = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryAmounts = new LinkedHashMap<>();
        Map<String, Integer> categoryCounts = new LinkedHashMap<>();

        Map<Integer, BigDecimal> seasonAmounts = new LinkedHashMap<>();
        Map<Integer, String> seasonNames = new LinkedHashMap<>();
        Map<Integer, Integer> seasonCounts = new LinkedHashMap<>();

        for (Expense e : expenses) {
            BigDecimal amt = e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO;
            grandTotal = grandTotal.add(amt);

            String cat = e.getExpenseCategory();
            categoryAmounts.put(cat, categoryAmounts.getOrDefault(cat, BigDecimal.ZERO).add(amt));
            categoryCounts.put(cat, categoryCounts.getOrDefault(cat, 0) + 1);

            if (e.getCropSeason() != null) {
                Integer sId = e.getCropSeason().getId();
                String sName = e.getCropSeason().getSeasonName();
                seasonAmounts.put(sId, seasonAmounts.getOrDefault(sId, BigDecimal.ZERO).add(amt));
                seasonNames.put(sId, sName);
                seasonCounts.put(sId, seasonCounts.getOrDefault(sId, 0) + 1);
            }
        }

        final BigDecimal finalGrandTotal = grandTotal;
        List<ExpenseSummaryResp.ExpenseCategorySummary> byCategory = categoryAmounts.entrySet().stream().map(entry -> {
            String cat = entry.getKey();
            BigDecimal amount = entry.getValue();
            int count = categoryCounts.getOrDefault(cat, 0);
            BigDecimal percentage = BigDecimal.ZERO;
            if (finalGrandTotal.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.multiply(BigDecimal.valueOf(100)).divide(finalGrandTotal, 2, RoundingMode.HALF_UP);
            }
            return new ExpenseSummaryResp.ExpenseCategorySummary(cat, amount, count, percentage);
        }).toList();

        List<ExpenseSummaryResp.ExpenseSeasonSummary> bySeason = seasonAmounts.entrySet().stream().map(entry -> {
            Integer sId = entry.getKey();
            String sName = seasonNames.get(sId);
            BigDecimal amount = entry.getValue();
            int count = seasonCounts.getOrDefault(sId, 0);
            return new ExpenseSummaryResp.ExpenseSeasonSummary(sId, sName, amount, count);
        }).toList();

        String seasonName = null;
        if (cropSeasonId != null) {
            seasonName = cropSeasonRepository.findById(cropSeasonId).map(CropSeason::getSeasonName).orElse(null);
        }

        return new ExpenseSummaryResp(
                farmId,
                cropSeasonId,
                seasonName,
                grandTotal,
                expenses.size(),
                byCategory,
                bySeason
        );
    }
}
