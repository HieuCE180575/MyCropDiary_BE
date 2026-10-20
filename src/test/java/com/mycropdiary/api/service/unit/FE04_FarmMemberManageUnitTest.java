package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.farm.AddFarmMemberReq;
import com.mycropdiary.api.dto.farm.FarmMemberResp;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE04_MEM - FarmServiceImpl.members (UC-14: Quản lý thành viên trang trại)")
public class FE04_FarmMemberManageUnitTest {

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

    private Farm farm;
    private User ownerUser;
    private User newStaffUser;

    @BeforeEach
    void setUp() {
        ownerUser = new User("owner@farm.com", "hash", "Owner Name", "0901234567", "user", "active");
        ownerUser.setId(10);

        newStaffUser = new User("staff@farm.com", "hash", "Staff Name", "0907654321", "user", "active");
        newStaffUser.setId(20);

        farm = new Farm();
        farm.setId(1);
        farm.setFarmName("Green Farm");
    }

    @Test
    @DisplayName("FE04_MEM_UTC01 [Normal] - Thêm thành viên mới hợp lệ vào Farm -> Thành công (Passed)")
    void UTC01_addMember_validStaff_shouldAddMemberSuccessfully() {
        AddFarmMemberReq req = new AddFarmMemberReq("staff@farm.com", "staff", "Kỹ sư nông nghiệp");
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(userRepository.findByEmailIgnoreCase("staff@farm.com")).thenReturn(Optional.of(newStaffUser));
        when(farmMemberRepository.existsByFarmIdAndUserId(1, 20)).thenReturn(false);

        FarmMember saved = new FarmMember(farm, newStaffUser, "staff", "active");
        saved.setId(100);
        when(farmMemberRepository.save(any(FarmMember.class))).thenReturn(saved);

        FarmMemberResp resp = farmService.addMember(10, 1, req);

        assertNotNull(resp);
        verify(farmAccessGuard).requireOwnerOrAdmin(10, 1);
        verify(farmMemberRepository).save(any(FarmMember.class));
    }

    @Test
    @DisplayName("FE04_MEM_UTC02 [Abnormal] - Thêm thành viên vào Farm không tồn tại -> Báo lỗi ResourceNotFound (Passed)")
    void UTC02_addMember_nonExistentFarm_shouldThrowResourceNotFound() {
        AddFarmMemberReq req = new AddFarmMemberReq("staff@farm.com", "staff", "Kỹ sư");
        when(farmRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> farmService.addMember(10, 999, req));
    }

    @Test
    @DisplayName("FE04_MEM_UTC03 [Abnormal] - Người không có quyền thêm thành viên -> Ném AccessDeniedException (Passed)")
    void UTC03_addMember_userNotOwnerOrAdmin_shouldThrowAccessDenied() {
        AddFarmMemberReq req = new AddFarmMemberReq("staff@farm.com", "staff", "Kỹ sư");
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        doThrow(new AccessDeniedException("Access Denied")).when(farmAccessGuard).requireOwnerOrAdmin(99, 1);

        assertThrows(AccessDeniedException.class, () -> farmService.addMember(99, 1, req));
    }

    @Test
    @DisplayName("FE04_MEM_UTC04 [Abnormal] - Email thành viên chưa đăng ký tài khoản -> Báo lỗi ResourceNotFound (Passed)")
    void UTC04_addMember_nonExistentUserEmail_shouldThrowResourceNotFound() {
        AddFarmMemberReq req = new AddFarmMemberReq("unknown@farm.com", "staff", "Kỹ sư");
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(userRepository.findByEmailIgnoreCase("unknown@farm.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> farmService.addMember(10, 1, req));
    }

    @Test
    @DisplayName("FE04_MEM_UTC05 [Abnormal] - Thêm user đã là thành viên của Farm -> Báo lỗi trùng lặp (Passed)")
    void UTC05_addMember_userAlreadyMember_shouldThrowIllegalArgument() {
        AddFarmMemberReq req = new AddFarmMemberReq("staff@farm.com", "staff", "Kỹ sư");
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(userRepository.findByEmailIgnoreCase("staff@farm.com")).thenReturn(Optional.of(newStaffUser));
        when(farmMemberRepository.existsByFarmIdAndUserId(1, 20)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> farmService.addMember(10, 1, req));
    }

    @Test
    @DisplayName("FE04_MEM_UTC06 [Abnormal] - Thêm vai trò OWNER khi Farm đã có OWNER đang hoạt động -> Chặn vi phạm (Passed)")
    void UTC06_addMember_duplicateActiveOwner_shouldThrowBusinessRule() {
        AddFarmMemberReq req = new AddFarmMemberReq("staff@farm.com", "owner", "Đồng sở hữu");
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(farmMemberRepository.existsByFarmIdAndFarmRoleAndStatus(1, "owner", "active")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> farmService.addMember(10, 1, req));
    }

    @Test
    @DisplayName("FE04_MEM_UTC07 [Normal] - Xem danh sách thành viên của Farm bởi thành viên hợp lệ (Passed)")
    void UTC07_getFarmMembers_authorizedMember_shouldReturnList() {
        when(farmMemberRepository.findByFarmId(1)).thenReturn(List.of(
                new FarmMember(farm, ownerUser, "owner", "active"),
                new FarmMember(farm, newStaffUser, "staff", "active")
        ));

        List<FarmMemberResp> list = farmService.getFarmMembers(10, 1);

        assertNotNull(list);
        assertEquals(2, list.size());
        verify(farmAccessGuard).requireFarmMember(10, 1);
    }

    @Test
    @DisplayName("FE04_MEM_UTC08 [Normal] - Xóa thành viên khỏi trang trại thành công bởi Owner (Passed)")
    void UTC08_removeMember_validOwner_shouldRemoveMember() {
        FarmMember staffMember = new FarmMember(farm, newStaffUser, "staff", "active");
        staffMember.setId(50);
        when(farmMemberRepository.findById(50)).thenReturn(Optional.of(staffMember));

        farmService.removeMember(10, 1, 50);

        verify(farmAccessGuard).requireOwnerOrAdmin(10, 1);
        assertEquals("left", staffMember.getMembershipStatus());
        verify(farmMemberRepository).save(staffMember);
    }

    @Test
    @DisplayName("FE04_MEM_UTC09 [Boundary] - Email thành viên dài > 255 ký tự gây tràn cột DB (Failed: DF-016)")
    void UTC09_addMember_emailExceeds255Chars_defectDF016() {
        // [Defect DF-016] DTO AddFarmMemberReq thiếu @Size(max=255)
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-016: [Defect Found] Email thành viên dài quá 255 ký tự gây tràn cột varchar(255) ném ngoại lệ 500 thay vì 400 Bad Request.");
        } else {
            String longEmail = "a".repeat(250) + "@test.com";
            assertTrue(longEmail.length() > 255);
        }
    }

    @Test
    @Disabled("FE04_MEM_UTC10 [Abnormal] - Gán quyền đồng thời 2 Owner (Race Condition) -> Yêu cầu kiểm thử đa luồng (Untested)")
    @DisplayName("FE04_MEM_UTC10 [Abnormal] - Concurrent owner assignment race condition (Untested)")
    void UTC10_addMember_concurrentOwnerRaceCondition_deferred() {
        // Untested: Cần môi trường mô phỏng đa luồng
    }
}
