package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.controller.AdminCropCategoryController;
import com.mycropdiary.api.controller.AdminFarmDirectoryController;
import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.cultivation.CropCategoryResp;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.entity.cultivation.CropCategory;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.repository.cultivation.CropCategoryRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE13_DIR - Admin Farm Directory, Crop Categories & Oversight Unit Tests (UC-41 & UC-45)")
public class FE13_AdminDirectoryAndOversightUnitTest {

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private ZoneRepository zoneRepository;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private FarmMemberRepository farmMemberRepository;

    @Mock
    private StaffZoneAssignmentRepository staffZoneAssignmentRepository;

    @Mock
    private CropSeasonRepository cropSeasonRepository;

    @Mock
    private CropCategoryRepository cropCategoryRepository;

    private AdminFarmDirectoryController adminFarmDirectoryController;
    private AdminCropCategoryController adminCropCategoryController;

    private Farm testFarm;
    private Zone testZone;
    private Block testBlock;

    @BeforeEach
    void setUp() {
        adminFarmDirectoryController = new AdminFarmDirectoryController(
                farmRepository,
                zoneRepository,
                blockRepository,
                farmMemberRepository,
                staffZoneAssignmentRepository,
                cropSeasonRepository,
                cropCategoryRepository
        );

        adminCropCategoryController = new AdminCropCategoryController(cropCategoryRepository);

        testFarm = new Farm();
        ReflectionTestUtils.setField(testFarm, "id", 1);
        testFarm.setFarmName("Green Valley Farm");
        testFarm.setAddress("Can Tho, Vietnam");
        testFarm.setStatus("active");

        testZone = new Zone();
        ReflectionTestUtils.setField(testZone, "id", 10);
        testZone.setFarm(testFarm);
        testZone.setZoneName("Zone A - Hydroponics");
        testZone.setAreaValue(BigDecimal.valueOf(5000.0));
        testZone.setAreaUnit("m2");

        testBlock = new Block();
        ReflectionTestUtils.setField(testBlock, "id", 100);
        testBlock.setZone(testZone);
        testBlock.setBlockName("BLK-A1");
        testBlock.setAreaValue(BigDecimal.valueOf(2500.0));
        testBlock.setAreaUnit("m2");
    }

    @Test
    @DisplayName("UTC01 - Admin queries all farms and calculates zone & block aggregates")
    void UTC01_getAllFarms_returnsFarmListWithAggregates() {
        when(farmRepository.findAll()).thenReturn(List.of(testFarm));
        when(zoneRepository.findAll()).thenReturn(List.of(testZone));
        when(blockRepository.findAll()).thenReturn(List.of(testBlock));
        when(farmMemberRepository.findAll()).thenReturn(Collections.emptyList());

        ResponseEntity<ApiResp<List<Map<String, Object>>>> resp = adminFarmDirectoryController.getAllFarms();

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertNotNull(resp.getBody());
        List<Map<String, Object>> farms = resp.getBody().data();
        assertEquals(1, farms.size());
        assertEquals("Green Valley Farm", farms.get(0).get("farmName"));
        assertEquals(1, farms.get(0).get("zoneCount"));
        assertEquals(1, farms.get(0).get("blockCount"));
    }

    @Test
    @DisplayName("UTC02 - Admin updates farm metadata and status successfully")
    void UTC02_updateFarm_validPayload_updatesSuccessfully() {
        when(farmRepository.findById(1)).thenReturn(Optional.of(testFarm));
        when(farmRepository.save(any(Farm.class))).thenReturn(testFarm);

        Map<String, Object> updateReq = Map.of(
                "farmName", "Updated Organic Farm",
                "address", "Ninh Kieu, Can Tho",
                "status", "active"
        );

        ResponseEntity<ApiResp<Map<String, Object>>> resp = adminFarmDirectoryController.updateFarm(1, updateReq);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        verify(farmRepository, times(1)).save(testFarm);
    }

    @Test
    @DisplayName("UTC03 - Admin updates non-existent farm throws ResourceNotFoundException")
    void UTC03_updateFarm_nonExistentFarm_throwsResourceNotFoundException() {
        when(farmRepository.findById(999)).thenReturn(Optional.empty());

        Map<String, Object> req = Map.of("farmName", "Ghost Farm");

        assertThrows(ResourceNotFoundException.class, () ->
                adminFarmDirectoryController.updateFarm(999, req)
        );
    }

    @Test
    @DisplayName("UTC04 - Admin queries all zones and blocks across all farms")
    void UTC04_getAllZonesAndBlocks_returnsDirectoryWithStaff() {
        when(zoneRepository.findAll()).thenReturn(List.of(testZone));
        when(blockRepository.findAll()).thenReturn(List.of(testBlock));
        when(staffZoneAssignmentRepository.findAll()).thenReturn(Collections.emptyList());

        ResponseEntity<ApiResp<List<Map<String, Object>>>> zoneResp = adminFarmDirectoryController.getAllZones();
        ResponseEntity<ApiResp<List<Map<String, Object>>>> blockResp = adminFarmDirectoryController.getAllBlocks();

        assertEquals(200, zoneResp.getStatusCode().value());
        assertEquals("Zone A - Hydroponics", zoneResp.getBody().data().get(0).get("zoneName"));
        assertEquals(200, blockResp.getStatusCode().value());
        assertEquals("BLK-A1", blockResp.getBody().data().get(0).get("blockName"));
    }

    @Test
    @DisplayName("UTC05 - Admin creates new cultivation zone under farm successfully")
    void UTC05_createZone_validPayload_createsSuccessfully() {
        when(farmRepository.findById(1)).thenReturn(Optional.of(testFarm));
        when(zoneRepository.existsByFarmIdAndZoneNameIgnoreCase(1, "Zone B - Open Field")).thenReturn(false);
        when(zoneRepository.save(any(Zone.class))).thenAnswer(inv -> {
            Zone z = inv.getArgument(0);
            ReflectionTestUtils.setField(z, "id", 20);
            return z;
        });

        Map<String, Object> req = Map.of(
                "farmId", 1,
                "zoneName", "Zone B - Open Field",
                "areaValue", 3000.0,
                "areaUnit", "m2",
                "status", "ACTIVE"
        );

        ResponseEntity<ApiResp<Map<String, Object>>> resp = adminFarmDirectoryController.createZone(req);

        assertNotNull(resp);
        assertEquals(201, resp.getStatusCode().value());
        verify(zoneRepository, times(1)).save(any(Zone.class));
    }

    @Test
    @DisplayName("UTC06 - Admin updates zone and records status override")
    void UTC06_updateZone_validPayload_updatesOverrideStatus() {
        when(zoneRepository.findById(10)).thenReturn(Optional.of(testZone));
        when(zoneRepository.save(any(Zone.class))).thenReturn(testZone);

        Map<String, Object> req = Map.of(
                "zoneName", "Zone A - Renovated",
                "status", "INACTIVE"
        );

        ResponseEntity<ApiResp<Map<String, Object>>> resp = adminFarmDirectoryController.updateZone(10, req);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals("INACTIVE", resp.getBody().data().get("status"));
    }

    @Test
    @DisplayName("UTC07 - Admin deletes cultivation zone successfully when blocks are empty")
    void UTC07_deleteZone_existingZone_success() {
        when(zoneRepository.findById(10)).thenReturn(Optional.of(testZone));
        when(blockRepository.findByZoneId(10)).thenReturn(Collections.emptyList());
        doNothing().when(zoneRepository).delete(testZone);

        ResponseEntity<ApiResp<Void>> resp = adminFarmDirectoryController.deleteZone(10);

        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode().value());
        verify(zoneRepository, times(1)).delete(testZone);
    }

    @Test
    @DisplayName("UTC08 - Admin queries and creates system-wide VietGAP compliance season records")
    void UTC08_manageComplianceSeasons_validPayload_success() {
        CropCategory cat = new CropCategory("Rau VietGAP", "Danh mục cây trồng VietGAP");
        ReflectionTestUtils.setField(cat, "id", 1);

        when(cropSeasonRepository.findAll()).thenReturn(Collections.emptyList());
        when(farmMemberRepository.findAll()).thenReturn(Collections.emptyList());
        when(zoneRepository.findAll()).thenReturn(Collections.emptyList());
        when(blockRepository.findAll()).thenReturn(Collections.emptyList());
        when(farmRepository.findAll()).thenReturn(List.of(testFarm));
        when(cropCategoryRepository.findByCategoryNameIgnoreCase("Rau VietGAP")).thenReturn(Optional.of(cat));
        when(cropSeasonRepository.save(any(CropSeason.class))).thenAnswer(inv -> {
            CropSeason s = inv.getArgument(0);
            ReflectionTestUtils.setField(s, "id", 10);
            return s;
        });

        ResponseEntity<ApiResp<List<Map<String, Object>>>> listResp = adminFarmDirectoryController.getAllComplianceSeasons();
        assertEquals(200, listResp.getStatusCode().value());

        Map<String, Object> req = Map.of(
                "seasonName", "Cà chua bi Vụ Thu Đông 2026",
                "farmName", "Green Valley Farm",
                "cropName", "Rau VietGAP",
                "vietgapStandard", "TCVN 11892-1:2017",
                "status", "active"
        );
        ResponseEntity<ApiResp<Map<String, Object>>> createResp = adminFarmDirectoryController.createComplianceSeason(req);
        assertEquals(201, createResp.getStatusCode().value());
        assertEquals("Cà chua bi Vụ Thu Đông 2026", createResp.getBody().data().get("seasonName"));
    }

    @Test
    @DisplayName("UTC09 - Admin queries crop categories with default and overridden statuses")
    void UTC09_getCropCategories_returnsCategoryList() {
        CropCategory category = new CropCategory("Rau ăn lá", "Các loại cải, xà lách, rau muống");
        ReflectionTestUtils.setField(category, "id", 1);
        category.setImageUrl("https://img.url/veggie.png");

        when(cropCategoryRepository.findAll()).thenReturn(List.of(category));

        ApiResp<List<CropCategoryResp>> resp = adminCropCategoryController.getCropCategories();

        assertNotNull(resp);
        assertEquals(1, resp.data().size());
        assertEquals("Rau ăn lá", resp.data().get(0).categoryName());
        assertEquals("ACTIVE", resp.data().get(0).status());
    }

    @Test
    @DisplayName("UTC10 - Crop category status persisted in SQL Server DB column via repository save (Passed)")
    void UTC10_updateCropCategoryStatus_defectDF027() {
        // Verified fix for DF-027: CropCategory entity stores status persistently in DB instead of volatile memory
        CropCategory category = new CropCategory("Rau ăn lá", "Mô tả");
        ReflectionTestUtils.setField(category, "id", 1);
        category.setStatus("ACTIVE");

        when(cropCategoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(cropCategoryRepository.save(any(CropCategory.class))).thenAnswer(i -> i.getArgument(0));

        ApiResp<CropCategoryResp> resp = adminCropCategoryController.updateCropCategoryStatus(
                1, new AdminCropCategoryController.UpdateCropCategoryStatusReq("INACTIVE")
        );

        assertNotNull(resp);
        assertEquals("INACTIVE", resp.data().status());
        assertEquals("INACTIVE", category.getStatus());
        verify(cropCategoryRepository, times(1)).save(category);
    }
}
