package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.expense.CreateExpenseReq;
import com.mycropdiary.api.dto.expense.ExpenseResp;
import com.mycropdiary.api.dto.expense.ExpenseSummaryResp;
import com.mycropdiary.api.dto.expense.UpdateExpenseReq;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseService {

    /**
     * View Expense List, Search Expenses, Filter Expenses:
     * Lấy danh sách chi phí sản xuất, tìm kiếm theo từ khóa và lọc theo mùa vụ, danh mục, khoảng thời gian
     */
    List<ExpenseResp> getExpenseList(
            Integer currentUserId,
            Integer farmId,
            Integer cropSeasonId,
            String category,
            LocalDate startDate,
            LocalDate endDate,
            String keyword
    );

    /**
     * View Expense Details: Xem chi tiết một khoản chi phí
     */
    ExpenseResp getExpenseDetails(Integer currentUserId, Integer expenseId);

    /**
     * Record Expense: Ghi nhận mới một khoản chi phí sản xuất
     */
    ExpenseResp recordExpense(Integer currentUserId, Integer farmId, CreateExpenseReq request);

    /**
     * Update Expense: Cập nhật thông tin chi phí
     */
    ExpenseResp updateExpense(Integer currentUserId, Integer expenseId, UpdateExpenseReq request);

    /**
     * Delete Eligible Expense: Xóa chi phí đủ điều kiện (yêu cầu Farm Owner, Manager hoặc người ghi nhận)
     */
    void deleteExpense(Integer currentUserId, Integer expenseId);

    /**
     * View Expense and Production Cost Summary: Báo cáo tổng hợp chi phí sản xuất theo danh mục và mùa vụ
     */
    ExpenseSummaryResp getExpenseSummary(
            Integer currentUserId,
            Integer farmId,
            Integer cropSeasonId,
            LocalDate startDate,
            LocalDate endDate
    );
}

