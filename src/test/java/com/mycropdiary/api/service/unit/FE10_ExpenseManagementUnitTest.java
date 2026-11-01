package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.expense.*;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.Expense;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.cultivation.ExpenseRepository;
import com.mycropdiary.api.repository.cultivation.FarmTaskRepository;
import com.mycropdiary.api.service.impl.ExpenseServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Expense & Production Cost Service Unit Tests")
public class FE10_ExpenseManagementUnitTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private CropSeasonRepository cropSeasonRepository;
    @Mock
    private FarmTaskRepository farmTaskRepository;
    @Mock
    private FarmMemberRepository farmMemberRepository;
    @Mock
    private FarmAccessGuard accessGuard;

    @InjectMocks
    private ExpenseServiceImpl expenseService;

    private Farm farm;
    private User user;
    private FarmMember ownerMember;
    private CropSeason season;
    private Expense expense1;
    private Expense expense2;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(1);

        user = new User("farmer@test.com", "passhash", "Nguyen Van A", "0901234567", "user", "active");
        user.setId(5);

        ownerMember = new FarmMember(farm, user, "owner", "active");
        ownerMember.setId(10);

        season = new CropSeason();
        season.setId(20);
        season.setFarm(farm);
        season.setSeasonName("Tomato Spring 2026");

        expense1 = new Expense(farm, season, null, "Materials & Fertilizers",
                "Soluble NPK fertilizers", new BigDecimal("18500000.00"), LocalDate.of(2026, 1, 5), ownerMember, null);
        expense1.setId(101);

        expense2 = new Expense(farm, season, null, "Labor",
                "Wages for greenhouse workers", new BigDecimal("4500000.00"), LocalDate.of(2026, 1, 14), ownerMember, null);
        expense2.setId(102);
    }

    @Test
    @DisplayName("Record Expense: Should record new expense successfully")
    void recordExpense_validInputs_shouldSucceed() {
        when(accessGuard.requireFarmAccess(5, 1)).thenReturn(ownerMember);
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 5, "active")).thenReturn(Optional.of(ownerMember));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense1);

        CreateExpenseReq req = new CreateExpenseReq(
                "Materials & Fertilizers",
                new BigDecimal("18500000.00"),
                LocalDate.of(2026, 1, 5),
                null, null,
                "Soluble NPK fertilizers",
                null
        );

        ExpenseResp resp = expenseService.recordExpense(5, 1, req);
        assertNotNull(resp);
        assertEquals(101, resp.expenseId());
        assertEquals("Materials & Fertilizers", resp.expenseCategory());
        assertEquals(new BigDecimal("18500000.00"), resp.amount());
    }

    @Test
    @DisplayName("Record Expense: Should reject negative expense amount")
    void recordExpense_negativeAmount_shouldThrowBadRequest() {
        when(accessGuard.requireFarmAccess(5, 1)).thenReturn(ownerMember);
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));

        CreateExpenseReq req = new CreateExpenseReq(
                "Labor",
                new BigDecimal("-500000.00"),
                LocalDate.now(),
                null, null, null, null
        );

        assertThrows(BadRequestException.class, () -> expenseService.recordExpense(5, 1, req));
    }

    @Test
    @DisplayName("View Expense List & Search: Should return list matching search keyword and category")
    void getExpenseList_withFilters_shouldReturnList() {
        when(accessGuard.requireFarmAccess(5, 1)).thenReturn(ownerMember);
        when(expenseRepository.searchExpenses(1, 20, "Labor", null, null, "workers"))
                .thenReturn(List.of(expense2));

        List<ExpenseResp> list = expenseService.getExpenseList(5, 1, 20, "Labor", null, null, "workers");
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Labor", list.get(0).expenseCategory());
        assertEquals(new BigDecimal("4500000.00"), list.get(0).amount());
    }

    @Test
    @DisplayName("Update Expense: Should update expense details successfully")
    void updateExpense_validInputs_shouldSucceed() {
        when(expenseRepository.findByIdWithDetails(101)).thenReturn(Optional.of(expense1));
        when(accessGuard.requireFarmAccess(5, 1)).thenReturn(ownerMember);
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateExpenseReq req = new UpdateExpenseReq(
                "Materials & Fertilizers",
                new BigDecimal("19000000.00"),
                LocalDate.of(2026, 1, 6),
                null, null,
                "Updated invoice description",
                "https://img.com/receipt.jpg"
        );

        ExpenseResp updated = expenseService.updateExpense(5, 101, req);
        assertNotNull(updated);
        assertEquals(new BigDecimal("19000000.00"), updated.amount());
        assertEquals("Updated invoice description", updated.description());
        assertEquals("https://img.com/receipt.jpg", updated.receiptImageUrl());
    }

    @Test
    @DisplayName("Delete Eligible Expense: Should succeed when user is Farm Owner")
    void deleteExpense_asOwner_shouldSucceed() {
        when(expenseRepository.findByIdWithDetails(101)).thenReturn(Optional.of(expense1));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 5, "active")).thenReturn(Optional.of(ownerMember));

        assertDoesNotThrow(() -> expenseService.deleteExpense(5, 101));
        verify(expenseRepository).delete(expense1);
    }

    @Test
    @DisplayName("View Expense and Production Cost Summary: Should aggregate amounts by category and season")
    void getExpenseSummary_shouldAggregateCorrectly() {
        when(accessGuard.requireFarmAccess(5, 1)).thenReturn(ownerMember);
        when(expenseRepository.findExpensesForSummary(1, null, null, null))
                .thenReturn(List.of(expense1, expense2));

        ExpenseSummaryResp summary = expenseService.getExpenseSummary(5, 1, null, null, null);
        assertNotNull(summary);
        assertEquals(new BigDecimal("23000000.00"), summary.totalExpenseAmount());
        assertEquals(2, summary.totalExpenseCount());
        assertEquals(2, summary.byCategory().size());
        assertEquals(1, summary.bySeason().size());
    }
}

