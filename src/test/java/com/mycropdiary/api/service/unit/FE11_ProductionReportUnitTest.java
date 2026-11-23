package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.report.CropSeasonReportResp;
import com.mycropdiary.api.dto.report.FarmReportResp;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.Expense;
import com.mycropdiary.api.entity.cultivation.HarvestRecord;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.cultivation.ExpenseRepository;
import com.mycropdiary.api.repository.cultivation.FarmingActivityRepository;
import com.mycropdiary.api.repository.cultivation.HarvestRecordRepository;
import com.mycropdiary.api.repository.cultivation.InputPurchaseRepository;
import com.mycropdiary.api.service.impl.ReportServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE11_RPT - Production Report Management Unit Tests (UC-30)")
public class FE11_ProductionReportUnitTest {

    @Mock
    private FarmRepository farmRepository;
    @Mock
    private CropSeasonRepository cropSeasonRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private HarvestRecordRepository harvestRecordRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private InputPurchaseRepository inputPurchaseRepository;
    @Mock
    private FarmingActivityRepository farmingActivityRepository;
    @Mock
    private FarmAccessGuard accessGuard;

    private ReportServiceImpl reportService;
    private Farm testFarm;

    @BeforeEach
    void setUp() {
        reportService = new ReportServiceImpl(
                farmRepository,
                cropSeasonRepository,
                blockRepository,
                harvestRecordRepository,
                expenseRepository,
                inputPurchaseRepository,
                farmingActivityRepository,
                accessGuard
        );

        testFarm = new Farm();
        testFarm.setId(1);
        testFarm.setFarmName("Trang trại VietGAP Đà Lạt");
    }

    @Test
    @DisplayName("FE11_RPT_UTC01 [Normal] - Tổng hợp báo cáo sản xuất toàn trang trại thành công")
    void UTC01_generateFarmReport_validFarm_shouldAggregateYieldAndExpenses() {
        HarvestRecord harvest = new HarvestRecord();
        harvest.setId(1);
        harvest.setQuantity(new BigDecimal("2500.000"));
        harvest.setUnit("kg");
        harvest.setQualityGrade("Loại 1 (Xuất khẩu)");

        Expense expense = new Expense();
        expense.setId(1);
        expense.setAmount(new BigDecimal("15000000.00"));
        expense.setExpenseCategory("Phân bón & Dinh dưỡng");

        CropSeason season = new CropSeason();
        season.setId(1);
        season.setSeasonName("Mùa dưa lưới 2026");
        season.setSeasonStatus("active");

        when(farmRepository.findById(1)).thenReturn(Optional.of(testFarm));
        when(harvestRecordRepository.findByFarmId(1)).thenReturn(List.of(harvest));
        when(expenseRepository.findExpensesForSummary(1, null, null, null)).thenReturn(List.of(expense));
        when(inputPurchaseRepository.findByFarmIdOrderByPurchaseDateDesc(1)).thenReturn(List.of());
        when(cropSeasonRepository.findByFarmIdOrderByStartDateDesc(1)).thenReturn(List.of(season));
        when(blockRepository.findByZoneFarmId(1)).thenReturn(List.of());

        FarmReportResp report = reportService.generateFarmReport(1, 1);

        assertNotNull(report);
        assertEquals("Trang trại VietGAP Đà Lạt", report.farmName());
        assertEquals(new BigDecimal("2500.000"), report.totalHarvestYield());
        assertEquals(1, report.totalHarvestBatches());
        assertEquals(100.0, report.gradeOnePercentage());
        assertEquals(new BigDecimal("15000000.00"), report.totalExpenseAmount());
    }

    @Test
    @DisplayName("FE11_RPT_UTC02 [Abnormal] - Tạo báo cáo cho trang trại không tồn tại ném ResourceNotFoundException")
    void UTC02_generateFarmReport_nonExistentFarm_shouldThrowResourceNotFound() {
        when(farmRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reportService.generateFarmReport(1, 999));
    }

    @Test
    @DisplayName("FE11_RPT_UTC03 [Boundary] - Báo cáo trang trại chưa có dữ liệu thu hoạch và chi phí trả về các chỉ số bằng 0")
    void UTC03_generateFarmReport_emptyData_shouldReturnZeroMetrics() {
        when(farmRepository.findById(1)).thenReturn(Optional.of(testFarm));
        when(harvestRecordRepository.findByFarmId(1)).thenReturn(List.of());
        when(expenseRepository.findExpensesForSummary(1, null, null, null)).thenReturn(List.of());
        when(inputPurchaseRepository.findByFarmIdOrderByPurchaseDateDesc(1)).thenReturn(List.of());
        when(cropSeasonRepository.findByFarmIdOrderByStartDateDesc(1)).thenReturn(List.of());
        when(blockRepository.findByZoneFarmId(1)).thenReturn(List.of());

        FarmReportResp report = reportService.generateFarmReport(1, 1);

        assertNotNull(report);
        assertEquals(BigDecimal.ZERO, report.totalHarvestYield());
        assertEquals(0.0, report.gradeOnePercentage());
        assertEquals(BigDecimal.ZERO, report.totalExpenseAmount());
    }

    @Test
    @DisplayName("FE11_RPT_UTC04 [Normal] - Xem báo cáo chi tiết mùa vụ canh tác thành công")
    void UTC04_getCropSeasonReport_validSeason_shouldReturnSeasonAnalytics() {
        CropSeason season = new CropSeason();
        season.setId(1);
        season.setFarm(testFarm);
        season.setSeasonName("Mùa dưa lưới 2026");
        season.setStartDate(LocalDate.of(2026, 1, 15));
        season.setSeasonStatus("active");

        when(cropSeasonRepository.findById(1)).thenReturn(Optional.of(season));
        when(harvestRecordRepository.findByCropSeasonId(1)).thenReturn(List.of());
        when(expenseRepository.findExpensesForSummary(1, 1, null, null)).thenReturn(List.of());
        when(farmingActivityRepository.countByCropSeasonId(1)).thenReturn(8L);

        CropSeasonReportResp report = reportService.getCropSeasonReport(1, 1);

        assertNotNull(report);
        assertEquals("Mùa dưa lưới 2026", report.seasonName());
        assertEquals(8, report.activityCount());
    }

    @Test
    @DisplayName("FE11_RPT_UTC05 [Abnormal] - Xem báo cáo mùa vụ không tồn tại ném ResourceNotFoundException")
    void UTC05_getCropSeasonReport_nonExistentSeason_shouldThrowResourceNotFound() {
        when(cropSeasonRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reportService.getCropSeasonReport(1, 999));
    }
}
