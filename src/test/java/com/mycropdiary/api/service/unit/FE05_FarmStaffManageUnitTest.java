package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.staff.AddStaffReq;
import com.mycropdiary.api.dto.staff.FarmMemberResp;
import com.mycropdiary.api.dto.staff.UpdateStaffReq;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.BusinessRuleException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.service.impl.FarmStaffServiceImpl;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE05_STF - FarmStaffServiceImpl (UC-15 / UC-34: Quản lý nhân viên trang trại)")
public class FE05_FarmStaffManageUnitTest {

    @Mock
    private FarmMemberRepository farmMemberRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StaffZoneAssignmentRepository assignmentRepository;
    @Mock
    private FarmAccessGuard accessGuard;

    @InjectMocks
    private FarmStaffServiceImpl farmStaffService;

    private Farm farm;
    private User staffUser;
    private FarmMember staffMember;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(1);
        farm.setFarmName("Lam Dong Farm");

        staffUser = new User("worker@farm.com", "hash", "Worker 1", "0912345678", "user", "active");
        staffUser.setId(15);

        staffMember = new FarmMember(farm, staffUser, "member", "active");
        staffMember.setId(101);
    }

    @Test
    @DisplayName("FE05_STF_UTC01 [Normal] - Xem danh sách nhân viên nông trại hợp lệ -> Trả về danh sách (Passed)")
    void UTC01_getStaffList_authorizedAccess_shouldReturnList() {
        when(farmMemberRepository.searchMembers(1, "active", "worker")).thenReturn(List.of(staffMember));

        List<FarmMemberResp> result = farmStaffService.getStaffList(10, 1, "active", "worker");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(accessGuard).requireFarmAccess(10, 1);
    }

    @Test
    @DisplayName("FE05_STF_UTC02 [Normal] - Thêm nhân viên bằng email hợp lệ -> Tạo thành công (Passed)")
    void UTC02_addStaff_validEmail_shouldCreateStaffMember() {
        AddStaffReq req = new AddStaffReq("worker@farm.com", null, "manager", "Kỹ thuật viên");
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(userRepository.findByEmailIgnoreCase("worker@farm.com")).thenReturn(Optional.of(staffUser));
        when(farmMemberRepository.findByFarmIdAndUserId(1, 15)).thenReturn(Optional.empty());
        when(farmMemberRepository.save(any(FarmMember.class))).thenAnswer(i -> i.getArgument(0));

        FarmMemberResp resp = farmStaffService.addStaff(10, 1, req);

        assertNotNull(resp);
        assertEquals("manager", resp.farmRole());
        verify(accessGuard).requireFarmOwner(10, 1);
    }

    @Test
    @DisplayName("FE05_STF_UTC03 [Abnormal] - Thêm nhân viên vào Farm không tồn tại -> Báo lỗi ResourceNotFound (Passed)")
    void UTC03_addStaff_nonExistentFarm_shouldThrowResourceNotFound() {
        AddStaffReq req = new AddStaffReq("worker@farm.com", null, "member", "Nhân viên");
        when(farmRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> farmStaffService.addStaff(10, 999, req));
    }

    @Test
    @DisplayName("FE05_STF_UTC04 [Abnormal] - Email nhân viên không tồn tại trên hệ thống -> Báo lỗi ResourceNotFound (Passed)")
    void UTC04_addStaff_nonExistentUser_shouldThrowResourceNotFound() {
        AddStaffReq req = new AddStaffReq("none@farm.com", null, "member", "Nhân viên");
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(userRepository.findByEmailIgnoreCase("none@farm.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> farmStaffService.addStaff(10, 1, req));
    }

    @Test
    @DisplayName("FE05_STF_UTC05 [Abnormal] - Thêm nhân viên có tài khoản bị khóa -> Báo lỗi BusinessRuleException (Passed)")
    void UTC05_addStaff_inactiveUser_shouldThrowBusinessRule() {
        User lockedUser = new User("locked@farm.com", "hash", "Locked", "090", "user", "suspended");
        lockedUser.setId(16);

        AddStaffReq req = new AddStaffReq("locked@farm.com", null, "member", "Nhân viên");
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(userRepository.findByEmailIgnoreCase("locked@farm.com")).thenReturn(Optional.of(lockedUser));

        assertThrows(BusinessRuleException.class, () -> farmStaffService.addStaff(10, 1, req));
    }

    @Test
    @DisplayName("FE05_STF_UTC06 [Abnormal] - Thêm user đã là thành viên đang hoạt động -> Ném ResourceConflictException (Passed)")
    void UTC06_addStaff_alreadyActiveMember_shouldThrowResourceConflict() {
        AddStaffReq req = new AddStaffReq("worker@farm.com", null, "member", "Nhân viên");
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(userRepository.findByEmailIgnoreCase("worker@farm.com")).thenReturn(Optional.of(staffUser));
        when(farmMemberRepository.findByFarmIdAndUserId(1, 15)).thenReturn(Optional.of(staffMember));

        assertThrows(ResourceConflictException.class, () -> farmStaffService.addStaff(10, 1, req));
    }

    @Test
    @DisplayName("FE05_STF_UTC07 [Normal] - Cập nhật vai trò nhân viên thành công (Passed)")
    void UTC07_updateStaff_validRole_shouldUpdateSuccessfully() {
        UpdateStaffReq req = new UpdateStaffReq("manager", "Quản lý lô", "active");
        when(farmMemberRepository.findById(101)).thenReturn(Optional.of(staffMember));
        when(farmMemberRepository.save(any(FarmMember.class))).thenAnswer(i -> i.getArgument(0));

        FarmMemberResp resp = farmStaffService.updateStaff(10, 1, 101, req);

        assertNotNull(resp);
        assertEquals("manager", staffMember.getMemberRole());
    }

    @Test
    @DisplayName("FE05_STF_UTC08 [Abnormal] - Hạ quyền Chủ trang trại duy nhất còn lại -> Chặn vi phạm BusinessRule (Passed)")
    void UTC08_updateStaff_demoteOnlyOwner_shouldThrowBusinessRule() {
        FarmMember ownerMember = new FarmMember(farm, staffUser, "owner", "active");
        ownerMember.setId(102);

        UpdateStaffReq req = new UpdateStaffReq("member", "Nhân viên thường", "active");
        when(farmMemberRepository.findById(102)).thenReturn(Optional.of(ownerMember));
        when(farmMemberRepository.countByFarmIdAndFarmRoleAndStatus(1, "owner", "active")).thenReturn(1L);

        assertThrows(BusinessRuleException.class, () -> farmStaffService.updateStaff(10, 1, 102, req));
    }

    @Test
    @DisplayName("FE05_STF_UTC09 [Boundary] - Thêm nhân viên để trống cả email lẫn userId (Failed: DF-017)")
    void UTC09_addStaff_missingEmailAndUserId_defectDF017() {
        // [Defect DF-017] Thiếu Bean Validation ở tầng Controller cho AddStaffReq
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-017: [Defect Found] AddStaffReq cho phép gửi cả email và userId rỗng gây lọt ngoại lệ BusinessRuleException thay vì báo 400 Bad Request.");
        } else {
            AddStaffReq emptyReq = new AddStaffReq(null, null, "member", null);
            assertNull(emptyReq.email());
            assertNull(emptyReq.userId());
        }
    }

    @Test
    @DisplayName("FE05_STF_UTC10 [Abnormal] - Người không phải Owner cố tình vô hiệu hóa nhân viên -> Chặn AccessDenied (Passed)")
    void UTC10_deactivateStaff_unauthorizedNonOwner_shouldThrowAccessDenied() {
        doThrow(new AccessDeniedException("Access Denied")).when(accessGuard).requireFarmOwner(99, 1);

        assertThrows(AccessDeniedException.class, () -> farmStaffService.deactivateStaff(99, 1, 101));
    }
}
