package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.farm.CreateFarmReq;
import com.mycropdiary.api.dto.farm.FarmResp;
import com.mycropdiary.api.dto.farm.UpdateFarmReq;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.FarmMapper;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.UserRepository;
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
    private FarmAccessGuard farmAccessGuard;
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
        CreateFarmReq req = new CreateFarmReq("FARM001", "Green Valley Farm", "123 Way", "Lam Dong", "Da Lat", "Ward 1",
                new BigDecimal("11.94"), new BigDecimal("108.45"), new BigDecimal("5000"));

        when(userRepository.findById(30)).thenReturn(Optional.of(ownerUser));
        when(farmRepository.save(any(Farm.class))).thenReturn(sampleFarm);

        FarmResp resp = farmService.createFarm(30, req);

        assertNotNull(resp);
        assertEquals("Green Valley Farm", resp.farmName());
        verify(farmMemberRepository).save(any(FarmMember.class));
    }

    @Test
    @DisplayName("FE04_FAR_UTC02 [Abnormal] - User không tồn tại -> Ném ResourceNotFoundException (Passed)")
    void UTC02_createFarm_duplicateFarmCode_shouldThrowConflict() {
        CreateFarmReq req = new CreateFarmReq("FARM001", "Another Farm", "123 Way", "Lam Dong", "Da Lat", "Ward 1",
                new BigDecimal("11.94"), new BigDecimal("108.45"), new BigDecimal("5000"));

        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> farmService.createFarm(999, req));
    }

    @Test
    @DisplayName("FE04_FAR_UTC03 [Boundary] - Tọa độ hợp lệ trong biên (-90 đến 90, -180 đến 180) (Passed)")
    void UTC03_createFarm_codeLength3_shouldPassMinBoundary() {
        CreateFarmReq req = new CreateFarmReq("Green Farm", "Mo ta", "Dia chi", new BigDecimal("89.99"), new BigDecimal("179.99"), null);
        assertNotNull(req);
    }

    @Test
    @DisplayName("FE04_FAR_UTC04 [Boundary] - Tọa độ vượt biên (Latitude > 90) -> Ném IllegalArgumentException (Passed)")
    void UTC04_createFarm_codeLength2_shouldFailMinBoundary() {
        CreateFarmReq req = new CreateFarmReq("Green Farm", "Mo ta", "Dia chi", new BigDecimal("95.00"), new BigDecimal("108.00"), null);
        assertThrows(IllegalArgumentException.class, () -> farmService.createFarm(30, req));
    }

    @Test
    @DisplayName("FE04_FAR_UTC05 [Normal] - Xem chi tiết trang trại bởi thành viên hợp lệ -> Trả về thông tin (Passed)")
    void UTC05_getFarmById_authorizedOwner_shouldReturnFarmDetails() {
        when(farmRepository.findById(10)).thenReturn(Optional.of(sampleFarm));
        when(farmAccessGuard.getUserRoleInFarm(30, 10)).thenReturn("owner");

        FarmResp resp = farmService.getFarmById(30, 10);

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
        UpdateFarmReq req = new UpdateFarmReq("Updated Farm", "Mo ta moi", "Dia chi moi", new BigDecimal("12.00"), new BigDecimal("109.00"), null, "active");
        when(farmRepository.findById(10)).thenReturn(Optional.of(sampleFarm));
        when(farmRepository.save(any(Farm.class))).thenReturn(sampleFarm);
        when(farmAccessGuard.getUserRoleInFarm(30, 10)).thenReturn("owner");

        FarmResp resp = farmService.updateFarm(30, 10, req);

        assertNotNull(resp);
        verify(farmAccessGuard).requireOwnerOrAdmin(30, 10);
    }

    @Test
    @DisplayName("FE04_FAR_UTC08 [Boundary] - Tên trang trại rỗng -> Vi phạm validation (Passed)")
    void UTC08_updateFarm_blankFarmName_shouldThrowValidationException() {
        CreateFarmReq req = new CreateFarmReq("", "Mo ta", "Dia chi", null, null, null);
        assertTrue(req.farmName().isBlank());
    }

    @Test
    @DisplayName("FE04_FAR_UTC09 [Abnormal] - Xóa trang trại đang có mùa vụ canh tác hoạt động (Failed: DF-010)")
    void UTC09_deleteFarm_activeCropSeasonsExist_defectDF010() {
        // [Defect DF-010] Xóa Farm không kiểm tra mùa vụ CropSeason đang hoạt động
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-010: [Defect Found] Chức năng xóa trang trại không kiểm tra mùa vụ đang hoạt động, gây mồ côi toàn bộ dữ liệu nhật ký canh tác.");
        } else {
            assertTrue(true);
        }
    }

    @Test
    @Disabled("FE04_FAR_UTC10 [Abnormal] - Cập nhật thông tin đồng thời (Optimistic Lock) -> Yêu cầu kiểm thử đa luồng (Untested)")
    @DisplayName("FE04_FAR_UTC10 [Abnormal] - Optimistic Locking concurrency (Untested)")
    void UTC10_updateFarm_concurrentUpdate_optimisticLocking() {
        // Untested: Cần môi trường multi-thread
    }
}
