package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.expense.CreateExpenseReq;
import com.mycropdiary.api.dto.expense.ExpenseResp;
import com.mycropdiary.api.dto.expense.ExpenseSummaryResp;
import com.mycropdiary.api.dto.expense.UpdateExpenseReq;
import com.mycropdiary.api.service.cultivation.ExpenseService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "09. Chi phí sản xuất (Farm Expenses & Production Cost)", description = "APIs quản lý, theo dõi, tìm kiếm, lọc và thống kê chi phí sản xuất và vận hành trang trại")
@SecurityRequirement(name = "BearerAuthentication")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final SecurityUtils securityUtils;

    public ExpenseController(ExpenseService expenseService, SecurityUtils securityUtils) {
        this.expenseService = expenseService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "View Expense List, Search Expenses, Filter Expenses",
            description = "Xem danh sách chi phí trang trại, hỗ trợ tìm kiếm theo từ khóa (mô tả, danh mục) và lọc theo mùa vụ, phân loại chi phí, khoảng thời gian.")
    @GetMapping("/farms/{farmId}/expenses")
    public ApiResp<List<ExpenseResp>> getExpenseList(
            @PathVariable Integer farmId,
            @RequestParam(required = false) Integer cropSeasonId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String keyword) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        List<ExpenseResp> list = expenseService.getExpenseList(currentUserId, farmId, cropSeasonId, category, startDate, endDate, keyword);
        return ApiResp.ok("Lấy danh sách chi phí sản xuất thành công.", list);
    }

    @Operation(summary = "View Expense Details", description = "Xem chi tiết một khoản chi phí sản xuất theo ID.")
    @GetMapping("/expenses/{expenseId}")
    public ApiResp<ExpenseResp> getExpenseDetails(@PathVariable Integer expenseId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        ExpenseResp details = expenseService.getExpenseDetails(currentUserId, expenseId);
        return ApiResp.ok("Chi tiết khoản chi phí.", details);
    }

    @Operation(summary = "Record Expense", description = "Ghi nhận một khoản chi phí sản xuất mới vào hệ thống.")
    @PostMapping("/farms/{farmId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<ExpenseResp> recordExpense(
            @PathVariable Integer farmId,
            @Valid @RequestBody CreateExpenseReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        ExpenseResp response = expenseService.recordExpense(currentUserId, farmId, request);
        return ApiResp.ok("Ghi nhận chi phí thành công.", response);
    }

    @Operation(summary = "Update Expense", description = "Cập nhật thông tin chi phí, số tiền, ngày chi hoặc hóa đơn.")
    @PutMapping("/expenses/{expenseId}")
    public ApiResp<ExpenseResp> updateExpense(
            @PathVariable Integer expenseId,
            @Valid @RequestBody UpdateExpenseReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        ExpenseResp response = expenseService.updateExpense(currentUserId, expenseId, request);
        return ApiResp.ok("Cập nhật chi phí thành công.", response);
    }

    @Operation(summary = "Delete Eligible Expense", description = "Xóa khoản chi phí đủ điều kiện khỏi hệ thống (yêu cầu Farm Owner, Manager hoặc người ghi nhận).")
    @DeleteMapping("/expenses/{expenseId}")
    public ApiResp<Void> deleteExpense(@PathVariable Integer expenseId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        expenseService.deleteExpense(currentUserId, expenseId);
        return ApiResp.ok("Đã xóa khoản chi phí thành công.", null);
    }

    @Operation(summary = "View Expense and Production Cost Summary",
            description = "Báo cáo tổng hợp chi phí sản xuất theo danh mục (phân bón, nhân công, năng lượng...) và theo từng mùa vụ.")
    @GetMapping("/farms/{farmId}/expenses/summary")
    public ApiResp<ExpenseSummaryResp> getExpenseSummary(
            @PathVariable Integer farmId,
            @RequestParam(required = false) Integer cropSeasonId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        ExpenseSummaryResp summary = expenseService.getExpenseSummary(currentUserId, farmId, cropSeasonId, startDate, endDate);
        return ApiResp.ok("Báo cáo tổng hợp chi phí sản xuất.", summary);
    }
}

