package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.expense.CreateExpenseReqDTO;
import com.mycropdiary.api.dto.expense.ExpenseRespDTO;
import com.mycropdiary.api.dto.expense.ExpenseSummaryRespDTO;
import com.mycropdiary.api.dto.expense.UpdateExpenseReqDTO;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseService {

    /**
     * View Expense List, Search Expenses, Filter Expenses:
     * Lấy danh sách chi phí sản xuất, tìm kiếm theo từ khóa và lọc theo mùa vụ, danh mục, khoảng thời gian
     */
    List<ExpenseRespDTO> getExpenseList(
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
    ExpenseRespDTO getExpenseDetails(Integer currentUserId, Integer expenseId);

    /**
     * Record Expense: Ghi nhận mới một khoản chi phí sản xuất
     */
    ExpenseRespDTO recordExpense(Integer currentUserId, Integer farmId, CreateExpenseReqDTO request);

    /**
     * Update Expense: Cập nhật thông tin chi phí
     */
    ExpenseRespDTO updateExpense(Integer currentUserId, Integer expenseId, UpdateExpenseReqDTO request);

    /**
     * Delete Eligible Expense: Xóa chi phí đủ điều kiện (yêu cầu Farm Owner, Manager hoặc người ghi nhận)
     */
    void deleteExpense(Integer currentUserId, Integer expenseId);

    /**
     * View Expense and Production Cost Summary: Báo cáo tổng hợp chi phí sản xuất theo danh mục và mùa vụ
     */
    ExpenseSummaryRespDTO getExpenseSummary(
            Integer currentUserId,
            Integer farmId,
            Integer cropSeasonId,
            LocalDate startDate,
            LocalDate endDate
    );
}

