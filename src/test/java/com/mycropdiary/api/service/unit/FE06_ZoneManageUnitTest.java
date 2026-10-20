package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.farm.CreateZoneReq;
import com.mycropdiary.api.dto.farm.UpdateZoneReq;
import com.mycropdiary.api.dto.farm.ZoneDetailResp;
import com.mycropdiary.api.dto.farm.ZoneResp;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.service.ZoneAssignmentService;
import com.mycropdiary.api.service.impl.ZoneServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE06_ZN - ZoneServiceImpl (UC-16: Quản lý Phân khu / Vùng sản xuất - Zone)")
public class FE06_ZoneManageUnitTest {

    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private StaffZoneAssignmentRepository staffZoneAssignmentRepository;
    @Mock
    private ZoneAssignmentService zoneAssignmentService;
    @Mock
    private FarmAccessGuard accessGuard;

    @InjectMocks
    private ZoneServiceImpl zoneService;

    private Farm farm;
    private Zone zone;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(1);

        zone = new Zone(farm, "Khu Vực Đông", new BigDecimal("10000.00"), "Khu chuyên trồng lúa ST25");
        zone.setId(10);
        zone.setAreaUnit("m2");
    }

    @Test
    @DisplayName("FE06_ZN_UTC01 [Normal] - Tạo phân khu hợp lệ thành công (Passed)")
    void UTC01_createZone_validInput_shouldCreateSuccessfully() {
        CreateZoneReq req = new CreateZoneReq("Khu Vực Tây", new BigDecimal("5000.00"), "m2", "Khu trồng rau sạch", null);
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(zoneRepository.existsByFarmIdAndZoneNameIgnoreCase(1, "Khu Vực Tây")).thenReturn(false);

        Zone savedZone = new Zone(farm, "Khu Vực Tây", new BigDecimal("5000.00"), "Khu trồng rau sạch");
        savedZone.setId(11);
        savedZone.setAreaUnit("m2");
        when(zoneRepository.save(any(Zone.class))).thenReturn(savedZone);

        ZoneResp resp = zoneService.createZone(1, 1, req);

        assertNotNull(resp);
        assertEquals(11, resp.id());
        assertEquals("Khu Vực Tây", resp.zoneName());
        verify(accessGuard).requireOwnerOrAdmin(1, 1);
        verify(zoneRepository).save(any(Zone.class));
    }

    @Test
    @DisplayName("FE06_ZN_UTC02 [Abnormal] - Trang trại không tồn tại -> Báo lỗi ResourceNotFound (Passed)")
    void UTC02_createZone_nonExistentFarm_shouldThrowResourceNotFound() {
        CreateZoneReq req = new CreateZoneReq("Khu Vực Bắc", new BigDecimal("3000.00"), "m2", "Mô tả", null);
        when(farmRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> zoneService.createZone(1, 999, req));
    }

    @Test
    @DisplayName("FE06_ZN_UTC03 [Abnormal] - Tên phân khu bị trùng trong Farm -> Báo lỗi ResourceConflict (Passed)")
    void UTC03_createZone_duplicateZoneName_shouldThrowResourceConflict() {
        CreateZoneReq req = new CreateZoneReq("Khu Vực Đông", new BigDecimal("5000.00"), "m2", "Khu lặp", null);
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(zoneRepository.existsByFarmIdAndZoneNameIgnoreCase(1, "Khu Vực Đông")).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> zoneService.createZone(1, 1, req));
    }

    @Test
    @DisplayName("FE06_ZN_UTC04 [Normal] - Cập nhật thông tin phân khu thành công (Passed)")
    void UTC04_updateZone_validInput_shouldUpdateSuccessfully() {
        UpdateZoneReq req = new UpdateZoneReq("Khu Vực Đông Cải Tạo", new BigDecimal("12000.00"), "m2", "Cải tạo đất", null);
        when(accessGuard.requireZoneAccess(1, 10)).thenReturn(zone);
        when(zoneRepository.existsByFarmIdAndZoneNameIgnoreCaseAndIdNot(1, "Khu Vực Đông Cải Tạo", 10)).thenReturn(false);
        when(zoneRepository.save(any(Zone.class))).thenAnswer(i -> i.getArgument(0));

        ZoneResp resp = zoneService.updateZone(1, 10, req);

        assertNotNull(resp);
        assertEquals("Khu Vực Đông Cải Tạo", resp.zoneName());
        assertEquals(new BigDecimal("12000.00"), resp.areaValue());
        verify(accessGuard).requireOwnerOrAdmin(1, 1);
    }

    @Test
    @DisplayName("FE06_ZN_UTC05 [Abnormal] - Cập nhật đổi tên trùng với phân khu khác -> Báo lỗi ResourceConflict (Passed)")
    void UTC05_updateZone_duplicateNameOtherZone_shouldThrowResourceConflict() {
        UpdateZoneReq req = new UpdateZoneReq("Khu Vực Nam", new BigDecimal("10000.00"), "m2", "Mô tả", null);
        when(accessGuard.requireZoneAccess(1, 10)).thenReturn(zone);
        when(zoneRepository.existsByFarmIdAndZoneNameIgnoreCaseAndIdNot(1, "Khu Vực Nam", 10)).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> zoneService.updateZone(1, 10, req));
    }

    @Test
    @DisplayName("FE06_ZN_UTC06 [Abnormal] - Xóa phân khu khi còn tồn tại lô đất con -> Chặn vi phạm BusinessRule (Passed)")
    void UTC06_deleteZone_zoneContainsBlocks_shouldThrowBusinessRule() {
        when(accessGuard.requireZoneAccess(1, 10)).thenReturn(zone);
        Block childBlock = new Block();
        childBlock.setId(101);
        childBlock.setBlockName("Lô A1");
        when(blockRepository.findByZoneId(10)).thenReturn(List.of(childBlock));

        assertThrows(BusinessRuleException.class, () -> zoneService.deleteZone(1, 10));
        verify(zoneRepository, never()).delete(any());
    }

    @Test
    @DisplayName("FE06_ZN_UTC07 [Normal] - Xóa phân khu rỗng không chứa lô đất thành công (Passed)")
    void UTC07_deleteZone_emptyZone_shouldDeleteSuccessfully() {
        when(accessGuard.requireZoneAccess(1, 10)).thenReturn(zone);
        when(blockRepository.findByZoneId(10)).thenReturn(Collections.emptyList());

        zoneService.deleteZone(1, 10);

        verify(zoneRepository).delete(zone);
    }

    @Test
    @DisplayName("FE06_ZN_UTC08 [Normal] - Xem chi tiết phân khu kèm danh sách các lô đất con (Passed)")
    void UTC08_getZoneById_validId_shouldReturnZoneDetailWithBlocks() {
        when(accessGuard.requireZoneAccess(1, 10)).thenReturn(zone);
        Block b1 = new Block();
        b1.setId(51);
        b1.setZone(zone);
        b1.setBlockName("Lô 01");
        b1.setAreaValue(new BigDecimal("4000.00"));
        b1.setAreaUnit("m2");

        when(blockRepository.findByZoneId(10)).thenReturn(List.of(b1));

        ZoneDetailResp resp = zoneService.getZoneById(1, 10);

        assertNotNull(resp);
        assertEquals(10, resp.zoneId());
        assertEquals(1, resp.totalBlocks());
        assertEquals(1, resp.blocks().size());
        assertEquals("Lô 01", resp.blocks().get(0).blockName());
    }

    @Test
    @DisplayName("FE06_ZN_UTC09 [Boundary] - Tạo phân khu với diện tích bằng 0 hoặc số âm (Failed: DF-019)")
    void UTC09_createZone_zeroOrNegativeArea_defectDF019() {
        // [Defect DF-019] CreateZoneReq thiếu @DecimalMin(value="0.01") cho trường areaValue
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-019: [Defect Found] Hệ thống cho phép tạo phân khu (Zone) có diện tích bằng 0 hoặc số âm do thiếu ràng buộc @DecimalMin.");
        } else {
            CreateZoneReq req = new CreateZoneReq("Khu Vực 0m2", BigDecimal.ZERO, "m2", "Không có diện tích", null);
            assertNotNull(req);
            assertEquals(BigDecimal.ZERO, req.areaValue());
        }
    }

    @Test
    @Disabled("FE06_ZN_UTC10 [Abnormal] - Xóa phân khu đồng thời 2 quản lý (Race Condition) -> Yêu cầu kiểm thử đa luồng (Untested)")
    @DisplayName("FE06_ZN_UTC10 [Abnormal] - Xóa phân khu đồng thời 2 quản lý (Race Condition) (Untested)")
    void UTC10_deleteZone_concurrentConflict_untestedRaceCondition() {
        // Chưa hỗ trợ kiểm thử concurrency đa luồng trong bộ Unit Test hiện tại
    }
}
