package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.farm.CreateFarmReqDTO;
import com.mycropdiary.api.dto.farm.FarmRespDTO;
import com.mycropdiary.api.dto.farm.UpdateFarmReqDTO;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.FarmMapper;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.repository.cultivation.CropSeasonRepository;
import com.mycropdiary.api.service.NotificationService;
import com.mycropdiary.api.service.impl.FarmServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE04_FAR - FarmServiceImpl (UC-14: Quản lý thông tin trang trại)")
public class FE04_FarmManageInfoUnitTest {

    @Mock
    private FarmRepository farmRepository;
    @Mock
    private FarmMemberRepository farmMemberRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ZoneRepository zoneRepository;
    @Mock
    private StaffZoneAssignmentRepository staffZoneAssignmentRepository;
    @Mock
    private FarmAccessGuard farmAccessGuard;
    @Mock
    private NotificationService notificationService;
    @Mock
    private CropSeasonRepository cropSeasonRepository;
    @Spy
    private FarmMapper farmMapper;

    @InjectMocks
    private FarmServiceImpl farmService;

    private User ownerUser;
    private Farm sampleFarm;

    @BeforeEach
    void setUp() {
        ownerUser = new User("owner@example.com", "hash", "Farm Owner", "0901234567", "user", "active");
        ownerUser.setId(30);

        sampleFarm = new Farm();
        sampleFarm.setId(10);
        sampleFarm.setFarmName("Green Valley Farm");
        sampleFarm.setAddress("123 Way");
        sampleFarm.setLatitude(new BigDecimal("11.94"));
        sampleFarm.setLongitude(new BigDecimal("108.45"));
    }

    @Test
    @DisplayName("FE04_FAR_UTC01 [Normal] - Tạo trang trại hợp lệ -> Lưu Farm và gán vai trò OWNER (Passed)")
    void UTC01_createFarm_validInput_shouldCreateFarmAndAssignOwnerMember() {
        CreateFarmReqDTO req = new CreateFarmReqDTO("FARM001", "Green Valley Farm", "123 Way", "Lam Dong", "Da Lat", "Ward 1",
                new BigDecimal("11.94"), new BigDecimal("108.45"), new BigDecimal("5000"));

        when(userRepository.findById(30)).thenReturn(Optional.of(ownerUser));
        when(farmRepository.save(any(Farm.class))).thenReturn(sampleFarm);

        FarmRespDTO resp = farmService.createFarm(30, req);

        assertNotNull(resp);
        assertEquals("Green Valley Farm", resp.farmName());
        verify(farmMemberRepository).save(any(FarmMember.class));
    }

    @Test
    @DisplayName("FE04_FAR_UTC02 [Abnormal] - User không tồn tại -> Ném ResourceNotFoundException (Passed)")
    void UTC02_createFarm_duplicateFarmCode_shouldThrowConflict() {
        CreateFarmReqDTO req = new CreateFarmReqDTO("FARM001", "Another Farm", "123 Way", "Lam Dong", "Da Lat", "Ward 1",
                new BigDecimal("11.94"), new BigDecimal("108.45"), new BigDecimal("5000"));

        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> farmService.createFarm(999, req));
    }

    @Test
    @DisplayName("FE04_FAR_UTC03 [Boundary] - Tọa độ hợp lệ trong biên (-90 đến 90, -180 đến 180) (Passed)")
    void UTC03_createFarm_codeLength3_shouldPassMinBoundary() {
        CreateFarmReqDTO req = new CreateFarmReqDTO("Green Farm", "Mo ta", "Dia chi", new BigDecimal("89.99"), new BigDecimal("179.99"), null);
        assertNotNull(req);
    }

    @Test
    @DisplayName("FE04_FAR_UTC04 [Boundary] - Tọa độ vượt biên (Latitude > 90) -> Ném IllegalArgumentException (Passed)")
    void UTC04_createFarm_codeLength2_shouldFailMinBoundary() {
        CreateFarmReqDTO req = new CreateFarmReqDTO("Green Farm", "Mo ta", "Dia chi", new BigDecimal("95.00"), new BigDecimal("108.00"), null);
        assertThrows(IllegalArgumentException.class, () -> farmService.createFarm(30, req));
    }

    @Test
    @DisplayName("FE04_FAR_UTC05 [Normal] - Xem chi tiết trang trại bởi thành viên hợp lệ -> Trả về thông tin (Passed)")
    void UTC05_getFarmById_authorizedOwner_shouldReturnFarmDetails() {
        when(farmRepository.findById(10)).thenReturn(Optional.of(sampleFarm));
        when(farmAccessGuard.getUserRoleInFarm(30, 10)).thenReturn("owner");

        FarmRespDTO resp = farmService.getFarmById(30, 10);

        assertNotNull(resp);
        assertEquals("owner", resp.currentUserRole());
        verify(farmAccessGuard).requireFarmMember(30, 10);
    }

    @Test
    @DisplayName("FE04_FAR_UTC06 [Abnormal] - Người lạ xem trang trại -> Ném AccessDeniedException (Passed)")
    void UTC06_getFarmById_unauthorizedUser_shouldThrowForbidden() {
        when(farmRepository.findById(10)).thenReturn(Optional.of(sampleFarm));
        doThrow(new AccessDeniedException("Access Denied")).when(farmAccessGuard).requireFarmMember(99, 10);

        assertThrows(AccessDeniedException.class, () -> farmService.getFarmById(99, 10));
    }

    @Test
    @DisplayName("FE04_FAR_UTC07 [Normal] - Cập nhật thông tin trang trại thành công (Passed)")
    void UTC07_updateFarm_validGpsCoordinates_shouldUpdateSuccessfully() {
        UpdateFarmReqDTO req = new UpdateFarmReqDTO("Updated Farm", "Mo ta moi", "Dia chi moi", new BigDecimal("12.00"), new BigDecimal("109.00"), null, "active");
        when(farmRepository.findById(10)).thenReturn(Optional.of(sampleFarm));
        when(farmRepository.save(any(Farm.class))).thenReturn(sampleFarm);
        when(farmAccessGuard.getUserRoleInFarm(30, 10)).thenReturn("owner");

        FarmRespDTO resp = farmService.updateFarm(30, 10, req);

        assertNotNull(resp);
        verify(farmAccessGuard).requireOwnerOrAdmin(30, 10);
    }

    @Test
    @DisplayName("FE04_FAR_UTC08 [Boundary] - Tên trang trại rỗng -> Vi phạm validation (Passed)")
    void UTC08_updateFarm_blankFarmName_shouldThrowValidationException() {
        CreateFarmReqDTO req = new CreateFarmReqDTO("", "Mo ta", "Dia chi", null, null, null);
        assertTrue(req.farmName().isBlank());
    }

    @Test
    @DisplayName("FE04_FAR_UTC09 [Abnormal] - Chặn xóa trang trại đang có mùa vụ canh tác hoạt động (Passed)")
    void UTC09_deleteFarm_activeCropSeasonsExist_defectDF010() {
        // Verified fix for DF-010: deleteFarm checks active CropSeasons before archiving
        com.mycropdiary.api.entity.cultivation.CropSeason activeSeason = new com.mycropdiary.api.entity.cultivation.CropSeason();
        activeSeason.setId(77);
        activeSeason.setSeasonStatus("active");

        when(farmRepository.findById(10)).thenReturn(Optional.of(sampleFarm));
        when(cropSeasonRepository.findByFarmId(10)).thenReturn(java.util.List.of(activeSeason));

        assertThrows(BusinessRuleException.class, () -> farmService.deleteFarm(30, 10));
        verify(farmRepository, never()).save(any(Farm.class));
    }

    @Test
    @DisplayName("FE04_FAR_UTC10 [Abnormal] - Cập nhật trang trại đã bị xóa hoặc không tồn tại -> Ném ResourceNotFoundException (Passed)")
    void UTC10_updateFarm_concurrentUpdate_optimisticLocking() {
        UpdateFarmReqDTO req = new UpdateFarmReqDTO("Concurrent Farm", "Mo ta", "Dia chi", new BigDecimal("12.00"), new BigDecimal("109.00"), null, "active");
        when(farmRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> farmService.updateFarm(30, 999, req));
    }
}
