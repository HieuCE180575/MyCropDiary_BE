package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.assignment.AssignmentResp;
import com.mycropdiary.api.dto.expense.ExpenseResp;
import com.mycropdiary.api.dto.expense.ExpenseSummaryResp;
import com.mycropdiary.api.dto.procurement.InputPurchaseResp;
import com.mycropdiary.api.dto.procurement.SupplierResp;
import com.mycropdiary.api.dto.staff.CandidateUserResp;
import com.mycropdiary.api.dto.staff.FarmMemberResp;
import com.mycropdiary.api.service.FarmStaffService;
import com.mycropdiary.api.service.ZoneAssignmentService;
import com.mycropdiary.api.service.cultivation.ExpenseService;
import com.mycropdiary.api.service.cultivation.InputPurchaseService;
import com.mycropdiary.api.service.cultivation.SupplierService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Farm Staff, Zone Assignment, Supplier, Purchase & Expense Unit Tests")
public class FE14_StaffProcurementExpenseUnitTest {

    @Mock
    private FarmStaffService farmStaffService;

    @Mock
    private ZoneAssignmentService zoneAssignmentService;

    @Mock
    private SupplierService supplierService;

    @Mock
    private InputPurchaseService inputPurchaseService;

    @Mock
    private ExpenseService expenseService;

    @Test
    @DisplayName("Manage Farm Staff - View Staff List")
    void testGetStaffList_success() {
        FarmMemberResp staff1 = new FarmMemberResp(
                1, 1, "Trang trai 1", 10, "Tran Van B", "staff_b@example.com",
                "0901234567", "staff", "Ky su nong nghiep", Instant.now(), null, "active", 0
        );
        when(farmStaffService.getStaffList(eq(1), eq(1), isNull(), isNull()))
                .thenReturn(List.of(staff1));

        List<FarmMemberResp> result = farmStaffService.getStaffList(1, 1, null, null);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Tran Van B", result.get(0).fullName());
        assertEquals("active", result.get(0).status());
    }

    @Test
    @DisplayName("Manage Farm Staff - Search Candidate Users")
    void testGetCandidateUsers_success() {
        CandidateUserResp candidate = new CandidateUserResp(
                15, "Candidate User", "candidate@example.com", "0988776655", null, "USER"
        );
        when(farmStaffService.getCandidateUsers(eq(1), eq(1), eq("candidate")))
                .thenReturn(List.of(candidate));

        List<CandidateUserResp> result = farmStaffService.getCandidateUsers(1, 1, "candidate");
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("candidate@example.com", result.get(0).email());
    }

    @Test
    @DisplayName("Zone Assignments - View Zone Assignment List")
    void testGetZoneAssignments_success() {
        AssignmentResp assignment = new AssignmentResp(
                101, 1, 10, "Tran Van B", "staff_b@example.com", "Ky su",
                1, "Khu nha mang 1", 1, "Owner", Instant.now(), null, true
        );
        when(zoneAssignmentService.getZoneAssignments(eq(1), eq(1), isNull()))
                .thenReturn(List.of(assignment));

        List<AssignmentResp> list = zoneAssignmentService.getZoneAssignments(1, 1, null);
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Khu nha mang 1", list.get(0).zoneName());
        assertTrue(list.get(0).isActive());
    }

    @Test
    @DisplayName("Supplier Management - View Supplier List")
    void testGetSuppliers_success() {
        SupplierResp supplier = new SupplierResp(
                1, 1, "Cong ty Vat tu Nong nghiep Xanh", "Nguyen Van A",
                "0912345678", "contact@xanh.com", "TP.HCM", "Ghi chu", Instant.now(), 5L
        );
        when(supplierService.getSuppliers(eq(1), eq(1), isNull()))
                .thenReturn(List.of(supplier));

        List<SupplierResp> list = supplierService.getSuppliers(1, 1, null);
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Cong ty Vat tu Nong nghiep Xanh", list.get(0).supplierName());
    }

    @Test
    @DisplayName("Supplier Management - View Supplier Details")
    void testGetSupplierDetails_success() {
        SupplierResp supplier = new SupplierResp(
                1, 1, "Cong ty Vat tu Nong nghiep Xanh", "Nguyen Van A",
                "0912345678", "contact@xanh.com", "TP.HCM", "Ghi chu", Instant.now(), 5L
        );
        when(supplierService.getSupplierDetails(eq(1), eq(1)))
                .thenReturn(supplier);

        SupplierResp details = supplierService.getSupplierDetails(1, 1);
        assertNotNull(details);
        assertEquals(1, details.supplierId());
        assertEquals("Nguyen Van A", details.contactName());
    }

    @Test
    @DisplayName("Input Purchases - View Purchase List")
    void testGetInputPurchases_success() {
        InputPurchaseResp purchase = new InputPurchaseResp(
                1, 1, 1, "Cong ty Xanh", 1, "Nguoi mua",
                LocalDate.now(), "HD-2026-001", BigDecimal.valueOf(15000000),
                null, null, "Ghi chu", Instant.now(), List.of()
        );
        when(inputPurchaseService.getInputPurchases(eq(1), eq(1)))
                .thenReturn(List.of(purchase));

        List<InputPurchaseResp> list = inputPurchaseService.getInputPurchases(1, 1);
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("HD-2026-001", list.get(0).referenceNo());
    }

    @Test
    @DisplayName("Input Purchases - Search & Filter Purchases")
    void testSearchInputPurchases_success() {
        InputPurchaseResp purchase = new InputPurchaseResp(
                2, 1, 1, "Cong ty Xanh", 1, "Nguoi mua",
                LocalDate.of(2026, 3, 1), "HD-2026-002", BigDecimal.valueOf(8000000),
                null, null, "Ghi chu", Instant.now(), List.of()
        );
        when(inputPurchaseService.searchInputPurchases(eq(1), eq(1), eq("dua luoi"), eq(1), any(), any()))
                .thenReturn(List.of(purchase));

        List<InputPurchaseResp> list = inputPurchaseService.searchInputPurchases(
                1, 1, "dua luoi", 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        );
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("HD-2026-002", list.get(0).referenceNo());
    }

    @Test
    @DisplayName("Expense Management - View Expense List & Filter")
    void testGetExpenseList_success() {
        ExpenseResp expense = new ExpenseResp(
                1, 1, 1, "Vu Xuan 2026", null, null,
                "Phan bon & Dinh duong", "Mua 5 bao phan huu co",
                BigDecimal.valueOf(2500000), LocalDate.now(), 1, "Nguoi ghi", null, Instant.now()
        );
        when(expenseService.getExpenseList(eq(1), eq(1), eq(1), eq("Phan bon & Dinh duong"), any(), any(), isNull()))
                .thenReturn(List.of(expense));

        List<ExpenseResp> list = expenseService.getExpenseList(
                1, 1, 1, "Phan bon & Dinh duong", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null
        );
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(BigDecimal.valueOf(2500000), list.get(0).amount());
    }

    @Test
    @DisplayName("Expense Management - View Expense Summary")
    void testGetExpenseSummary_success() {
        ExpenseSummaryResp summary = new ExpenseSummaryResp(
                1, null, null, BigDecimal.valueOf(35000000), 12, List.of(), List.of()
        );
        when(expenseService.getExpenseSummary(eq(1), eq(1), eq(1), any(), any()))
                .thenReturn(summary);

        ExpenseSummaryResp result = expenseService.getExpenseSummary(
                1, 1, 1, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)
        );
        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(35000000), result.totalExpenseAmount());
        assertEquals(12, result.totalExpenseCount());
    }
}
