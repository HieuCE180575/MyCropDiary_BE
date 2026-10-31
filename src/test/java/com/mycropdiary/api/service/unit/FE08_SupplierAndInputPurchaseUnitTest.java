package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.procurement.*;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.InputPurchase;
import com.mycropdiary.api.entity.cultivation.InputPurchaseItem;
import com.mycropdiary.api.entity.cultivation.Material;
import com.mycropdiary.api.entity.cultivation.Supplier;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.InputPurchaseItemRepository;
import com.mycropdiary.api.repository.cultivation.InputPurchaseRepository;
import com.mycropdiary.api.repository.cultivation.MaterialRepository;
import com.mycropdiary.api.repository.cultivation.MaterialUsageRepository;
import com.mycropdiary.api.repository.cultivation.SupplierRepository;
import com.mycropdiary.api.service.impl.InputPurchaseServiceImpl;
import com.mycropdiary.api.service.impl.SupplierServiceImpl;
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
@DisplayName("Supplier & Input Purchase Service Unit Tests (UC-35 & UC-36)")
public class FE08_SupplierAndInputPurchaseUnitTest {

    @Mock
    private SupplierRepository supplierRepository;
    @Mock
    private InputPurchaseRepository inputPurchaseRepository;
    @Mock
    private InputPurchaseItemRepository inputPurchaseItemRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private MaterialUsageRepository materialUsageRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private FarmMemberRepository farmMemberRepository;
    @Mock
    private FarmAccessGuard accessGuard;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    @InjectMocks
    private InputPurchaseServiceImpl inputPurchaseService;

    private Farm farm;
    private Supplier supplier;
    private Material material;
    private FarmMember member;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(1);

        supplier = new Supplier(farm, "Da Lat Green Agrochemical Co.", "Nguyen Van B", "0988111222", "dalat@agri.vn", "Da Lat City", "Trusted supplier");
        supplier.setId(10);

        material = new Material();
        material.setId(5);
        material.setFarm(farm);
        material.setMaterialName("Organic NPK 15-15-15");
        material.setUnit("kg");

        User user = new User("owner@farm.com", "hash", "Owner Name", "0912345678", "user", "active");
        user.setId(3);

        member = new FarmMember(farm, user, "owner", "active");
        member.setId(2);
    }

    // --- UC-35: Supplier Tests ---

    @Test
    @DisplayName("Create Supplier: Should create successfully")
    void createSupplier_valid_shouldSucceed() {
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(supplierRepository.existsByFarmIdAndSupplierNameIgnoreCase(1, "New Supplier")).thenReturn(false);

        Supplier saved = new Supplier(farm, "New Supplier", "Contact A", "0900000000", null, null, null);
        saved.setId(11);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(saved);

        CreateSupplierReq req = new CreateSupplierReq("New Supplier", "Contact A", "0900000000", null, null, null);
        SupplierResp resp = supplierService.createSupplier(3, 1, req);

        assertNotNull(resp);
        assertEquals(11, resp.supplierId());
        assertEquals("New Supplier", resp.supplierName());
    }

    @Test
    @DisplayName("Create Supplier: Should reject duplicate name in farm")
    void createSupplier_duplicateName_shouldThrowConflict() {
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(supplierRepository.existsByFarmIdAndSupplierNameIgnoreCase(1, "Da Lat Green Agrochemical Co.")).thenReturn(true);

        CreateSupplierReq req = new CreateSupplierReq("Da Lat Green Agrochemical Co.", null, null, null, null, null);
        assertThrows(ResourceConflictException.class, () -> supplierService.createSupplier(3, 1, req));
    }

    @Test
    @DisplayName("Delete Eligible Supplier: Should reject deletion when purchases exist")
    void deleteSupplier_hasPurchases_shouldThrowBadRequest() {
        when(supplierRepository.findById(10)).thenReturn(Optional.of(supplier));
        when(inputPurchaseRepository.existsBySupplierId(10)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> supplierService.deleteSupplier(3, 10));
        verify(supplierRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Delete Eligible Supplier: Should delete successfully when no purchases exist")
    void deleteSupplier_noPurchases_shouldDelete() {
        when(supplierRepository.findById(10)).thenReturn(Optional.of(supplier));
        when(inputPurchaseRepository.existsBySupplierId(10)).thenReturn(false);

        supplierService.deleteSupplier(3, 10);
        verify(supplierRepository, times(1)).delete(supplier);
    }

    // --- UC-36: Input Purchase Tests ---

    @Test
    @DisplayName("Record Input Purchase: Should record purchase and calculate total amount")
    void recordInputPurchase_valid_shouldCalculateTotal() {
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(supplierRepository.findById(10)).thenReturn(Optional.of(supplier));
        when(materialRepository.findById(5)).thenReturn(Optional.of(material));
        when(farmMemberRepository.findByFarmIdAndUserIdAndStatus(1, 3, "active")).thenReturn(Optional.of(member));

        InputPurchase savedPurchase = new InputPurchase(farm, supplier, member, LocalDate.now(), "PO-001", BigDecimal.ZERO, null, null);
        savedPurchase.setId(100);
        when(inputPurchaseRepository.save(any(InputPurchase.class))).thenReturn(savedPurchase);

        PurchaseItemReq itemReq = new PurchaseItemReq(5, new BigDecimal("20.000"), new BigDecimal("50000.00"), 1);
        CreateInputPurchaseReq req = new CreateInputPurchaseReq(
                10, LocalDate.now(), "PO-001", null, "NK-001", "Early Cantaloupe fertilizer", List.of(itemReq)
        );

        InputPurchaseItem savedItem = new InputPurchaseItem(savedPurchase, material, 1, new BigDecimal("20.000"), new BigDecimal("50000.00"));
        savedItem.setId(1);
        when(inputPurchaseItemRepository.save(any(InputPurchaseItem.class))).thenReturn(savedItem);

        InputPurchaseResp resp = inputPurchaseService.recordInputPurchase(3, 1, req);
        assertNotNull(resp);
        assertEquals(100, resp.purchaseId());
        assertEquals(1, resp.items().size());
        assertEquals(0, resp.totalAmount().compareTo(new BigDecimal("1000000")));
    }

    @Test
    @DisplayName("Delete Eligible Input Purchase: Should reject deletion when material is used in MaterialUsage")
    void deleteInputPurchase_materialInUse_shouldThrowBadRequest() {
        InputPurchase purchase = new InputPurchase(farm, supplier, member, LocalDate.now(), "PO-001", BigDecimal.ZERO, null, null);
        purchase.setId(100);

        InputPurchaseItem item = new InputPurchaseItem(purchase, material, 1, new BigDecimal("10.0"), new BigDecimal("20000.0"));
        when(inputPurchaseRepository.findById(100)).thenReturn(Optional.of(purchase));
        when(inputPurchaseItemRepository.findByInputPurchaseIdWithMaterial(100)).thenReturn(List.of(item));
        when(materialUsageRepository.existsByMaterialId(5)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> inputPurchaseService.deleteInputPurchase(3, 100));
        verify(inputPurchaseRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Link Warehouse Receipt: Should update warehouse receipt number")
    void linkWarehouseReceipt_valid_shouldUpdateReceiptNo() {
        InputPurchase purchase = new InputPurchase(farm, supplier, member, LocalDate.now(), "PO-001", BigDecimal.ZERO, null, null);
        purchase.setId(100);

        when(inputPurchaseRepository.findById(100)).thenReturn(Optional.of(purchase));
        when(inputPurchaseRepository.save(any(InputPurchase.class))).thenAnswer(inv -> inv.getArgument(0));

        LinkWarehouseReceiptReq req = new LinkWarehouseReceiptReq("WH-RECEIPT-2026-99", 1, "Completed stock entry");
        InputPurchaseResp resp = inputPurchaseService.linkWarehouseReceipt(3, 100, req);

        assertNotNull(resp);
        assertEquals("WH-RECEIPT-2026-99", resp.warehouseReceiptNo());
        assertTrue(resp.notes().contains("Completed stock entry"));
    }
}
