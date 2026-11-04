package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.report.CropSeasonReportResp;
import com.mycropdiary.api.dto.report.FarmReportResp;
import com.mycropdiary.api.dto.warehouse.CreateWarehouseReq;
import com.mycropdiary.api.dto.warehouse.InventoryItemResp;
import com.mycropdiary.api.dto.warehouse.UpdateInventoryReq;
import com.mycropdiary.api.dto.warehouse.WarehouseResp;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.cultivation.*;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.*;
import com.mycropdiary.api.service.impl.ReportServiceImpl;
import com.mycropdiary.api.service.impl.WarehouseServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Warehouse, Inventory & Report Management Unit Tests")
public class FE12_WarehouseAndReportUnitTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private InventoryRepository inventoryRepository;

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

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    private ReportServiceImpl reportService;

    private Farm testFarm;
    private Warehouse testWarehouse;
    private Material testMaterial;
    private Inventory testInventory;

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

        testWarehouse = new Warehouse(testFarm, "Kho trung tâm", "Cơ sở 1", null);
        testWarehouse.setId(1);

        testMaterial = new Material();
        testMaterial.setId(1);
        testMaterial.setMaterialName("Phân trùn quế");
        testMaterial.setUnit("kg");

        testInventory = new Inventory(testWarehouse, testMaterial, new BigDecimal("450.000"), new BigDecimal("100.000"));
        testInventory.setId(1);
    }

    @Test
    @DisplayName("UTC01 - Lấy danh sách kho hàng của trang trại thành công")
    void testGetWarehousesByFarm_success() {
        when(warehouseRepository.findByFarmIdOrderByWarehouseNameAsc(1)).thenReturn(List.of(testWarehouse));

        List<WarehouseResp> result = warehouseService.getWarehousesByFarm(1, 1);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Kho trung tâm", result.get(0).warehouseName());
        verify(accessGuard).requireFarmAccess(1, 1);
    }

    @Test
    @DisplayName("UTC02 - Tạo kho hàng mới thành công")
    void testCreateWarehouse_success() {
        CreateWarehouseReq req = new CreateWarehouseReq("Kho lạnh số 2", "Đà Lạt", null);
        when(farmRepository.findById(1)).thenReturn(Optional.of(testFarm));
        when(warehouseRepository.existsByFarmIdAndWarehouseNameIgnoreCase(1, "Kho lạnh số 2")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(invocation -> {
            Warehouse saved = invocation.getArgument(0);
            saved.setId(2);
            return saved;
        });

        WarehouseResp result = warehouseService.createWarehouse(1, 1, req);

        assertNotNull(result);
        assertEquals(2, result.warehouseId());
        assertEquals("Kho lạnh số 2", result.warehouseName());
    }

    @Test
    @DisplayName("UTC03 - Lấy danh sách tồn kho vật tư thành công")
    void testGetInventoriesByFarm_success() {
        when(inventoryRepository.findByFarmId(1)).thenReturn(List.of(testInventory));

        List<InventoryItemResp> result = warehouseService.getInventoriesByFarm(1, 1, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Phân trùn quế", result.get(0).materialName());
        assertEquals(new BigDecimal("450.000"), result.get(0).quantityOnHand());
    }

    @Test
    @DisplayName("UTC04 - Cập nhật số lượng tồn kho thành công")
    void testUpdateInventory_success() {
        UpdateInventoryReq req = new UpdateInventoryReq(new BigDecimal("500.000"), new BigDecimal("120.000"));
        when(inventoryRepository.findById(1)).thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(Inventory.class))).thenReturn(testInventory);

        InventoryItemResp result = warehouseService.updateInventory(1, 1, req);

        assertNotNull(result);
        assertEquals(new BigDecimal("500.000"), testInventory.getQuantityOnHand());
        assertEquals(new BigDecimal("120.000"), testInventory.getReorderLevel());
    }

    @Test
    @DisplayName("UTC05 - Tạo báo cáo tổng hợp trang trại thành công")
    void testGenerateFarmReport_success() {
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
        assertFalse(report.expenseBreakdown().isEmpty());
    }

    @Test
    @DisplayName("UTC06 - Xem báo cáo mùa vụ canh tác thành công")
    void testGetCropSeasonReport_success() {
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
}

