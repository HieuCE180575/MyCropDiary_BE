package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.cultivation.CreateMaterialUsageReqDTO;
import com.mycropdiary.api.dto.cultivation.MaterialUsageRespDTO;
import com.mycropdiary.api.dto.cultivation.UpdateMaterialUsageReqDTO;
import com.mycropdiary.api.dto.procurement.CreatePurchaseReqDTO;
import com.mycropdiary.api.dto.procurement.InputPurchaseRespDTO;
import com.mycropdiary.api.dto.procurement.PurchaseItemReqDTO;
import com.mycropdiary.api.dto.procurement.UpdatePurchaseReqDTO;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.FarmingActivity;
import com.mycropdiary.api.entity.cultivation.InputPurchase;
import com.mycropdiary.api.entity.cultivation.InputPurchaseItem;
import com.mycropdiary.api.entity.cultivation.Inventory;
import com.mycropdiary.api.entity.cultivation.Material;
import com.mycropdiary.api.entity.cultivation.MaterialUsage;
import com.mycropdiary.api.entity.cultivation.Warehouse;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.repository.cultivation.FarmingActivityRepository;
import com.mycropdiary.api.repository.cultivation.InputPurchaseItemRepository;
import com.mycropdiary.api.repository.cultivation.InputPurchaseRepository;
import com.mycropdiary.api.repository.cultivation.InventoryRepository;
import com.mycropdiary.api.repository.cultivation.MaterialRepository;
import com.mycropdiary.api.repository.cultivation.MaterialUsageRepository;
import com.mycropdiary.api.repository.cultivation.SupplierRepository;
import com.mycropdiary.api.repository.cultivation.WarehouseRepository;
import com.mycropdiary.api.service.impl.InputPurchaseServiceImpl;
import com.mycropdiary.api.service.impl.MaterialUsageServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE14_SYNC - Auto Inventory Stock Sync on Input Purchase & Material Usage Unit Tests (d8ff735)")
public class FE14_InventoryAutoSyncUnitTest {

    @Mock private InputPurchaseRepository inputPurchaseRepository;
    @Mock private InputPurchaseItemRepository inputPurchaseItemRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private MaterialUsageRepository materialUsageRepository;
    @Mock private FarmRepository farmRepository;
    @Mock private FarmMemberRepository farmMemberRepository;
    @Mock private FarmAccessGuard accessGuard;
    @Mock private InventoryRepository inventoryRepository;
    @Mock private WarehouseRepository warehouseRepository;
    @Mock private FarmingActivityRepository farmingActivityRepository;
    @Mock private CropSeasonRepository cropSeasonRepository;

    private InputPurchaseServiceImpl inputPurchaseService;
    private MaterialUsageServiceImpl materialUsageService;

    private Farm farm;
    private User ownerUser;
    private FarmMember ownerMember;
    private Warehouse warehouseA;
    private Warehouse warehouseB;
    private Material fertilizerMat;
    private Material pesticideMat;
    private FarmingActivity activity;

    @BeforeEach
    void setUp() {
        inputPurchaseService = new InputPurchaseServiceImpl(
                inputPurchaseRepository, inputPurchaseItemRepository, supplierRepository,
                materialRepository, materialUsageRepository, farmRepository,
                farmMemberRepository, accessGuard
        );
        ReflectionTestUtils.setField(inputPurchaseService, "inventoryRepository", inventoryRepository);
        ReflectionTestUtils.setField(inputPurchaseService, "warehouseRepository", warehouseRepository);

        materialUsageService = new MaterialUsageServiceImpl(
                materialUsageRepository, materialRepository, farmingActivityRepository,
                farmMemberRepository, cropSeasonRepository, farmRepository
        );
        ReflectionTestUtils.setField(materialUsageService, "inventoryRepository", inventoryRepository);

        farm = new Farm();
        farm.setId(10);
        farm.setFarmName("Nông trại Hậu Giang");

        ownerUser = new User("owner@farm.vn", "hash", "Chủ Trại", "0901234567", "owner", "active");
        ownerUser.setId(100);

        ownerMember = new FarmMember(farm, ownerUser, "owner", "active");
        ownerMember.setId(20);

        warehouseA = new Warehouse(farm, "Kho A - Phân bón", "Khu 1", null);
        warehouseA.setId(1);
        warehouseB = new Warehouse(farm, "Kho B - Dự phòng", "Khu 2", null);
        warehouseB.setId(2);

        fertilizerMat = new Material(farm, "Phân NPK 16-16-8", "Phân bón", "Dạng hạt", "kg");
        fertilizerMat.setId(50);
        fertilizerMat.setIsActive(true);

        pesticideMat = new Material(farm, "Chế phẩm Neem", "Thuốc BVTV", "Dạng nhũ dầu", "L");
        pesticideMat.setId(51);
        pesticideMat.setIsActive(true);

        Zone zone = new Zone();
        zone.setId(1);
        zone.setFarm(farm);

        Block block = new Block();
        block.setId(1);
        block.setZone(zone);

        CropSeason season = new CropSeason();
        season.setId(30);
        season.setFarm(farm);
        season.setPlot(block);
        season.setSeasonStatus("active");

        activity = new FarmingActivity();
        activity.setId(40);
        activity.setFarm(farm);
        activity.setCropSeason(season);
        activity.setBlock(block);
    }

    @Test
    @DisplayName("FE14_SYNC_UTC01 [Normal] - Ghi nhận phiếu mua vật tư tự động cộng tồn kho hiện có trong kho chỉ định")
    void UTC01_recordInputPurchase_incrementsExistingWarehouseInventory() {
        Inventory existingInv = new Inventory(warehouseA, fertilizerMat, new BigDecimal("100.000"), BigDecimal.ZERO);
        existingInv.setId(500);

        CreatePurchaseReqDTO req = new CreatePurchaseReqDTO(
                null, LocalDate.of(2026, 11, 27), "PO-001", null, "NK-001", "Mua phân",
                List.of(new PurchaseItemReqDTO(50, new BigDecimal("50.000"), new BigDecimal("25000"), 1))
        );
        when(farmRepository.findById(10)).thenReturn(Optional.of(farm));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(10, 100, "active")).thenReturn(Optional.of(ownerMember));
        when(inputPurchaseRepository.save(any(InputPurchase.class))).thenAnswer(i -> {
            InputPurchase p = i.getArgument(0);
            if (p.getId() == null) p.setId(901);
            return p;
        });
        when(materialRepository.findById(50)).thenReturn(Optional.of(fertilizerMat));
        when(inputPurchaseItemRepository.save(any(InputPurchaseItem.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryRepository.findByWarehouseIdAndMaterialId(1, 50)).thenReturn(Optional.of(existingInv));

        InputPurchaseRespDTO resp = inputPurchaseService.recordInputPurchase(100, 10, req);

        assertNotNull(resp);
        assertEquals(0, new BigDecimal("150.000").compareTo(existingInv.getQuantityOnHand()));
        verify(inventoryRepository).save(existingInv);
    }

    @Test
    @DisplayName("FE14_SYNC_UTC02 [Normal] - Ghi nhận phiếu mua vật tư chưa có tồn kho -> Tự động tạo bản ghi Inventory trong kho chỉ định")
    void UTC02_recordInputPurchase_autoCreatesInventoryInTargetWarehouse() {
        CreatePurchaseReqDTO req = new CreatePurchaseReqDTO(
                null, LocalDate.of(2026, 11, 27), "PO-002", null, null, null,
                List.of(new PurchaseItemReqDTO(50, new BigDecimal("80.000"), new BigDecimal("20000"), 2))
        );
        when(farmRepository.findById(10)).thenReturn(Optional.of(farm));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(10, 100, "active")).thenReturn(Optional.of(ownerMember));
        when(inputPurchaseRepository.save(any(InputPurchase.class))).thenAnswer(i -> {
            InputPurchase p = i.getArgument(0);
            if (p.getId() == null) p.setId(902);
            return p;
        });
        when(materialRepository.findById(50)).thenReturn(Optional.of(fertilizerMat));
        when(inputPurchaseItemRepository.save(any(InputPurchaseItem.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryRepository.findByWarehouseIdAndMaterialId(2, 50)).thenReturn(Optional.empty());
        when(inventoryRepository.findByFarmId(10)).thenReturn(List.of());
        when(warehouseRepository.findById(2)).thenReturn(Optional.of(warehouseB));

        inputPurchaseService.recordInputPurchase(100, 10, req);

        ArgumentCaptor<Inventory> invCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(invCaptor.capture());
        Inventory created = invCaptor.getValue();
        assertEquals(warehouseB, created.getWarehouse());
        assertEquals(fertilizerMat, created.getMaterial());
        assertEquals(0, new BigDecimal("80.000").compareTo(created.getQuantityOnHand()));
    }

    @Test
    @DisplayName("FE14_SYNC_UTC03 [Normal] - Phiếu mua không chọn kho -> Tự động fallback về kho đầu tiên theo tên của trang trại")
    void UTC03_recordInputPurchase_withoutWarehouseId_fallsBackToFirstFarmWarehouse() {
        CreatePurchaseReqDTO req = new CreatePurchaseReqDTO(
                null, LocalDate.of(2026, 11, 27), "PO-003", null, null, null,
                List.of(new PurchaseItemReqDTO(50, new BigDecimal("40.000"), new BigDecimal("20000"), null))
        );
        when(farmRepository.findById(10)).thenReturn(Optional.of(farm));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(10, 100, "active")).thenReturn(Optional.of(ownerMember));
        when(inputPurchaseRepository.save(any(InputPurchase.class))).thenAnswer(i -> {
            InputPurchase p = i.getArgument(0);
            if (p.getId() == null) p.setId(903);
            return p;
        });
        when(materialRepository.findById(50)).thenReturn(Optional.of(fertilizerMat));
        when(inputPurchaseItemRepository.save(any(InputPurchaseItem.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryRepository.findByFarmId(10)).thenReturn(List.of());
        when(warehouseRepository.findByFarmIdOrderByWarehouseNameAsc(10)).thenReturn(List.of(warehouseA, warehouseB));

        inputPurchaseService.recordInputPurchase(100, 10, req);

        ArgumentCaptor<Inventory> invCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(invCaptor.capture());
        assertEquals(warehouseA, invCaptor.getValue().getWarehouse());
        assertEquals(0, new BigDecimal("40.000").compareTo(invCaptor.getValue().getQuantityOnHand()));
    }

    @Test
    @DisplayName("FE14_SYNC_UTC04 [Normal] - Cập nhật phiếu mua hoàn trả số lượng cũ và cộng số lượng mới vào tồn kho")
    void UTC04_updateInputPurchase_reversesOldItemsAndAppliesNewStock() {
        InputPurchase purchase = new InputPurchase(farm, null, ownerMember, LocalDate.of(2026, 11, 20), "PO-004", new BigDecimal("1000000"), null, null);
        purchase.setId(904);
        InputPurchaseItem oldItem = new InputPurchaseItem(purchase, fertilizerMat, 1, new BigDecimal("50.000"), new BigDecimal("20000"));
        Inventory inv = new Inventory(warehouseA, fertilizerMat, new BigDecimal("100.000"), BigDecimal.ZERO);

        UpdatePurchaseReqDTO req = new UpdatePurchaseReqDTO(
                null, null, null, null, null, "Sửa số lượng",
                List.of(new PurchaseItemReqDTO(50, new BigDecimal("120.000"), new BigDecimal("20000"), 1))
        );
        when(inputPurchaseRepository.findById(904)).thenReturn(Optional.of(purchase));
        when(inputPurchaseItemRepository.findByInputPurchaseIdWithMaterial(904)).thenReturn(List.of(oldItem));
        when(materialRepository.findById(50)).thenReturn(Optional.of(fertilizerMat));
        when(inputPurchaseItemRepository.save(any(InputPurchaseItem.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryRepository.findByWarehouseIdAndMaterialId(1, 50)).thenReturn(Optional.of(inv));
        when(inputPurchaseRepository.save(any(InputPurchase.class))).thenAnswer(i -> i.getArgument(0));

        inputPurchaseService.updateInputPurchase(100, 904, req);

        assertEquals(0, new BigDecimal("170.000").compareTo(inv.getQuantityOnHand()));
        verify(inventoryRepository, times(2)).save(inv);
    }

    @Test
    @DisplayName("FE14_SYNC_UTC05 [Normal] - Xóa phiếu mua chưa xuất dùng tự động trừ hoàn tồn kho tương ứng")
    void UTC05_deleteInputPurchase_unusedMaterial_reversesStock() {
        InputPurchase purchase = new InputPurchase(farm, null, ownerMember, LocalDate.of(2026, 11, 20), "PO-005", BigDecimal.TEN, null, null);
        purchase.setId(905);
        InputPurchaseItem item = new InputPurchaseItem(purchase, fertilizerMat, 1, new BigDecimal("60.000"), new BigDecimal("10000"));
        Inventory inv = new Inventory(warehouseA, fertilizerMat, new BigDecimal("100.000"), BigDecimal.ZERO);

        when(inputPurchaseRepository.findById(905)).thenReturn(Optional.of(purchase));
        when(inputPurchaseItemRepository.findByInputPurchaseIdWithMaterial(905)).thenReturn(List.of(item));
        when(materialUsageRepository.existsByMaterialId(50)).thenReturn(false);
        when(inventoryRepository.findByWarehouseIdAndMaterialId(1, 50)).thenReturn(Optional.of(inv));

        inputPurchaseService.deleteInputPurchase(100, 905);

        assertEquals(0, new BigDecimal("40.000").compareTo(inv.getQuantityOnHand()));
        verify(inventoryRepository).save(inv);
        verify(inputPurchaseRepository).delete(purchase);
    }

    @Test
    @DisplayName("FE14_SYNC_UTC06 [Boundary] - Xóa phiếu mua có số lượng lớn hơn tồn hiện tại -> Tồn kho sàn về 0 (không âm)")
    void UTC06_deleteInputPurchase_floorsStockAtZero() {
        InputPurchase purchase = new InputPurchase(farm, null, ownerMember, LocalDate.of(2026, 11, 20), "PO-006", BigDecimal.TEN, null, null);
        purchase.setId(906);
        InputPurchaseItem item = new InputPurchaseItem(purchase, fertilizerMat, 1, new BigDecimal("50.000"), new BigDecimal("10000"));
        Inventory inv = new Inventory(warehouseA, fertilizerMat, new BigDecimal("20.000"), BigDecimal.ZERO);

        when(inputPurchaseRepository.findById(906)).thenReturn(Optional.of(purchase));
        when(inputPurchaseItemRepository.findByInputPurchaseIdWithMaterial(906)).thenReturn(List.of(item));
        when(materialUsageRepository.existsByMaterialId(50)).thenReturn(false);
        when(inventoryRepository.findByWarehouseIdAndMaterialId(1, 50)).thenReturn(Optional.of(inv));

        inputPurchaseService.deleteInputPurchase(100, 906);

        assertEquals(0, BigDecimal.ZERO.compareTo(inv.getQuantityOnHand()));
        verify(inventoryRepository).save(inv);
    }

    @Test
    @DisplayName("FE14_SYNC_UTC07 [Abnormal] - Xóa phiếu mua có vật tư đã xuất dùng trong nhật ký -> Chặn BadRequestException & không đổi tồn kho")
    void UTC07_deleteInputPurchase_usedMaterial_throwsBadRequestWithoutStockChange() {
        InputPurchase purchase = new InputPurchase(farm, null, ownerMember, LocalDate.of(2026, 11, 20), "PO-007", BigDecimal.TEN, null, null);
        purchase.setId(907);
        InputPurchaseItem item = new InputPurchaseItem(purchase, fertilizerMat, 1, new BigDecimal("50.000"), new BigDecimal("10000"));

        when(inputPurchaseRepository.findById(907)).thenReturn(Optional.of(purchase));
        when(inputPurchaseItemRepository.findByInputPurchaseIdWithMaterial(907)).thenReturn(List.of(item));
        when(materialUsageRepository.existsByMaterialId(50)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> inputPurchaseService.deleteInputPurchase(100, 907));
        verify(inventoryRepository, never()).save(any());
        verify(inputPurchaseRepository, never()).delete(any());
    }

    @Test
    @DisplayName("FE14_SYNC_UTC08 [Normal] - Ghi nhận sử dụng vật tư (500 g -> 0.5 kg) tự động trừ tồn kho đúng theo đơn vị chuẩn hóa")
    void UTC08_createMaterialUsage_deductsConvertedQuantityFromInventory() {
        Inventory inv = new Inventory(warehouseA, fertilizerMat, new BigDecimal("10.000"), BigDecimal.ZERO);
        CreateMaterialUsageReqDTO req = new CreateMaterialUsageReqDTO(
                40, 50, null, LocalDateTime.now().minusMinutes(10),
                new BigDecimal("500"), "g", "50g/gốc", "Bón gốc", 0, "Test g -> kg"
        );
        when(farmingActivityRepository.findById(40)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(10, 100, "active")).thenReturn(Optional.of(ownerMember));
        when(materialRepository.findById(50)).thenReturn(Optional.of(fertilizerMat));
        when(materialUsageRepository.save(any(MaterialUsage.class))).thenAnswer(i -> {
            MaterialUsage u = i.getArgument(0);
            u.setId(701);
            return u;
        });
        when(inventoryRepository.findByFarmId(10)).thenReturn(List.of(inv));

        MaterialUsageRespDTO resp = materialUsageService.createMaterialUsage(100, req);

        assertNotNull(resp);
        assertEquals(0, new BigDecimal("9.500").compareTo(inv.getQuantityOnHand()));
        verify(inventoryRepository).save(inv);
    }

    @Test
    @DisplayName("FE14_SYNC_UTC09 [Normal] - Sử dụng vật tư vượt tồn 1 kho -> Tự động trừ theo thứ tự tồn kho giảm dần qua nhiều kho")
    void UTC09_createMaterialUsage_deductsAcrossMultipleWarehousesLargestFirst() {
        Inventory invSmall = new Inventory(warehouseA, fertilizerMat, new BigDecimal("20.000"), BigDecimal.ZERO);
        Inventory invLarge = new Inventory(warehouseB, fertilizerMat, new BigDecimal("30.000"), BigDecimal.ZERO);
        CreateMaterialUsageReqDTO req = new CreateMaterialUsageReqDTO(
                40, 50, null, LocalDateTime.now().minusMinutes(5),
                new BigDecimal("40.000"), "kg", "1kg/luống", "Rải đều", 0, "Trừ liên kho"
        );
        when(farmingActivityRepository.findById(40)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(10, 100, "active")).thenReturn(Optional.of(ownerMember));
        when(materialRepository.findById(50)).thenReturn(Optional.of(fertilizerMat));
        when(materialUsageRepository.save(any(MaterialUsage.class))).thenAnswer(i -> {
            MaterialUsage u = i.getArgument(0);
            u.setId(702);
            return u;
        });
        when(inventoryRepository.findByFarmId(10)).thenReturn(List.of(invSmall, invLarge));

        materialUsageService.createMaterialUsage(100, req);

        assertEquals(0, BigDecimal.ZERO.compareTo(invLarge.getQuantityOnHand()));
        assertEquals(0, new BigDecimal("10.000").compareTo(invSmall.getQuantityOnHand()));
        verify(inventoryRepository, times(2)).save(any(Inventory.class));
    }

    @Test
    @DisplayName("FE14_SYNC_UTC10 [Boundary] - Sử dụng vật tư vượt tổng tồn tất cả kho -> Lưu nhật ký & sàn tồn kho về 0 (không chặn lỗi)")
    void UTC10_createMaterialUsage_exceedingTotalStock_floorsAtZero() {
        Inventory inv = new Inventory(warehouseA, fertilizerMat, new BigDecimal("30.000"), BigDecimal.ZERO);
        CreateMaterialUsageReqDTO req = new CreateMaterialUsageReqDTO(
                40, 50, null, LocalDateTime.now().minusMinutes(5),
                new BigDecimal("100.000"), "kg", "2kg/luống", "Bón lót", 0, "Vượt tồn"
        );
        when(farmingActivityRepository.findById(40)).thenReturn(Optional.of(activity));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(10, 100, "active")).thenReturn(Optional.of(ownerMember));
        when(materialRepository.findById(50)).thenReturn(Optional.of(fertilizerMat));
        when(materialUsageRepository.save(any(MaterialUsage.class))).thenAnswer(i -> {
            MaterialUsage u = i.getArgument(0);
            u.setId(703);
            return u;
        });
        when(inventoryRepository.findByFarmId(10)).thenReturn(List.of(inv));

        MaterialUsageRespDTO resp = materialUsageService.createMaterialUsage(100, req);

        assertNotNull(resp);
        assertEquals(0, BigDecimal.ZERO.compareTo(inv.getQuantityOnHand()));
        verify(inventoryRepository).save(inv);
    }

    @Test
    @DisplayName("FE14_SYNC_UTC11 [Normal] - Cập nhật nhật ký sử dụng đổi vật tư & số lượng -> Hoàn tồn vật tư cũ và trừ tồn vật tư mới")
    void UTC11_updateMaterialUsage_switchesMaterial_restoresOldAndDeductsNew() {
        MaterialUsage existing = new MaterialUsage();
        existing.setId(704);
        existing.setFarm(farm);
        existing.setFarmingActivity(activity);
        existing.setMaterial(fertilizerMat);
        existing.setQuantity(new BigDecimal("10.000"));

        Inventory invFert = new Inventory(warehouseA, fertilizerMat, new BigDecimal("40.000"), BigDecimal.ZERO);
        Inventory invPest = new Inventory(warehouseA, pesticideMat, new BigDecimal("50.000"), BigDecimal.ZERO);

        UpdateMaterialUsageReqDTO req = new UpdateMaterialUsageReqDTO(
                51, LocalDateTime.now().minusMinutes(2), new BigDecimal("5.000"), "L",
                "10ml/bình", "Phun lá", 7, "Đổi sang thuốc BVTV"
        );
        when(materialUsageRepository.findById(704)).thenReturn(Optional.of(existing));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(10, 100, "active")).thenReturn(Optional.of(ownerMember));
        when(materialRepository.findById(51)).thenReturn(Optional.of(pesticideMat));
        when(materialUsageRepository.save(any(MaterialUsage.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryRepository.findByFarmId(10)).thenReturn(List.of(invFert, invPest));

        materialUsageService.updateMaterialUsage(100, 704, req);

        assertEquals(0, new BigDecimal("50.000").compareTo(invFert.getQuantityOnHand()));
        assertEquals(0, new BigDecimal("45.000").compareTo(invPest.getQuantityOnHand()));
    }

    @Test
    @DisplayName("FE14_SYNC_UTC12 [Normal] - Xóa nhật ký sử dụng vật tư tự động cộng hoàn số lượng về tồn kho")
    void UTC12_deleteMaterialUsage_restoresDeductedStock() {
        MaterialUsage existing = new MaterialUsage();
        existing.setId(705);
        existing.setFarm(farm);
        existing.setFarmingActivity(activity);
        existing.setMaterial(fertilizerMat);
        existing.setQuantity(new BigDecimal("15.000"));

        Inventory invFert = new Inventory(warehouseA, fertilizerMat, new BigDecimal("40.000"), BigDecimal.ZERO);

        when(materialUsageRepository.findById(705)).thenReturn(Optional.of(existing));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(10, 100, "active")).thenReturn(Optional.of(ownerMember));
        when(inventoryRepository.findByFarmId(10)).thenReturn(List.of(invFert));

        materialUsageService.deleteMaterialUsage(100, 705);

        assertEquals(0, new BigDecimal("55.000").compareTo(invFert.getQuantityOnHand()));
        verify(materialUsageRepository).delete(existing);
        verify(inventoryRepository).save(invFert);
    }
}
