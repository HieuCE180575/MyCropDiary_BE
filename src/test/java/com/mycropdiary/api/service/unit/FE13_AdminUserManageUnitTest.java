package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.admin.UpdateUserStatusReq;
import com.mycropdiary.api.dto.admin.UserResp;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE13_ACC - AdminUserServiceImpl (UC-40: Quản trị tài khoản người dùng & Khóa/Mở khóa)")
public class FE13_AdminUserManageUnitTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private User adminUser;
    private User normalUser;
    private User lockedUser;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1);
        adminUser.setFullName("Super Admin");
        adminUser.setEmail("admin@mycropdiary.com");
        adminUser.setPhone("0988888888");
        adminUser.setSystemRole("admin");
        adminUser.setAccountStatus("active");

        normalUser = new User();
        normalUser.setId(10);
        normalUser.setFullName("Nguyen Van Farmer");
        normalUser.setEmail("farmer@example.com");
        normalUser.setPhone("0912345678");
        normalUser.setSystemRole("user");
        normalUser.setAccountStatus("active");

        lockedUser = new User();
        lockedUser.setId(20);
        lockedUser.setFullName("Spammer User");
        lockedUser.setEmail("spammer@example.com");
        lockedUser.setPhone("0999999999");
        lockedUser.setSystemRole("user");
        lockedUser.setAccountStatus("locked");

        pageable = PageRequest.of(0, 10);
    }

    @Test
    @DisplayName("FE13_ACC_UTC01 [Normal] - Xem danh sách tài khoản không điều kiện lọc -> Trả về trang UserResp (Passed)")
    void UTC01_getUsers_noFilter_shouldReturnAllUsersPage() {
        Page<User> mockPage = new PageImpl<>(List.of(adminUser, normalUser), pageable, 2);
        when(userRepository.searchUsers(null, null, pageable)).thenReturn(mockPage);

        PageResp<UserResp> result = adminUserService.getUsers(null, null, pageable);

        assertNotNull(result);
        assertEquals(2, result.totalElements());
        assertEquals(2, result.items().size());
        assertEquals("admin@mycropdiary.com", result.items().get(0).email());
        assertEquals("farmer@example.com", result.items().get(1).email());
    }

    @Test
    @DisplayName("FE13_ACC_UTC02 [Normal] - Lọc danh sách tài khoản theo trạng thái 'locked' -> Trả về các tài khoản bị khóa (Passed)")
    void UTC02_getUsers_filterByLockedStatus_shouldReturnLockedUsers() {
        Page<User> mockPage = new PageImpl<>(List.of(lockedUser), pageable, 1);
        when(userRepository.searchUsers("locked", null, pageable)).thenReturn(mockPage);

        PageResp<UserResp> result = adminUserService.getUsers("locked", null, pageable);

        assertNotNull(result);
        assertEquals(1, result.totalElements());
        assertEquals("LOCKED", result.items().get(0).accountStatus());
    }

    @Test
    @DisplayName("FE13_ACC_UTC03 [Normal] - Tìm kiếm tài khoản theo từ khóa tên hoặc email -> Trả về kết quả khớp (Passed)")
    void UTC03_getUsers_searchKeyword_shouldReturnMatchingUsers() {
        Page<User> mockPage = new PageImpl<>(List.of(normalUser), pageable, 1);
        when(userRepository.searchUsers(null, "farmer", pageable)).thenReturn(mockPage);

        PageResp<UserResp> result = adminUserService.getUsers(null, "farmer", pageable);

        assertNotNull(result);
        assertEquals(1, result.totalElements());
        assertEquals("farmer@example.com", result.items().get(0).email());
    }

    @Test
    @DisplayName("FE13_ACC_UTC04 [Normal] - Xem chi tiết tài khoản theo ID hợp lệ -> Trả về UserResp đầy đủ (Passed)")
    void UTC04_getUserById_validId_shouldReturnUserDetails() {
        when(userRepository.findById(10)).thenReturn(Optional.of(normalUser));

        UserResp resp = adminUserService.getUserById(10);

        assertNotNull(resp);
        assertEquals(10, resp.userId());
        assertEquals("farmer@example.com", resp.email());
        assertEquals("Nguyen Van Farmer", resp.fullName());
        assertEquals("ACTIVE", resp.accountStatus());
    }

    @Test
    @DisplayName("FE13_ACC_UTC05 [Abnormal] - Xem chi tiết tài khoản với ID không tồn tại -> Ném ResourceNotFoundException (Passed)")
    void UTC05_getUserById_nonExistentId_shouldThrowResourceNotFound() {
        when(userRepository.findById(9999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminUserService.getUserById(9999));
    }

    @Test
    @DisplayName("FE13_ACC_UTC06 [Normal] - Khóa tài khoản người dùng vi phạm (status='locked') -> Cập nhật trạng thái thành công (Passed)")
    void UTC06_updateUserStatus_lockUser_shouldUpdateStatusToLocked() {
        when(userRepository.findById(10)).thenReturn(Optional.of(normalUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UpdateUserStatusReq req = new UpdateUserStatusReq("locked");
        UserResp resp = adminUserService.updateUserStatus(1, 10, req);

        assertNotNull(resp);
        assertEquals("LOCKED", resp.accountStatus());
        verify(userRepository).save(normalUser);
    }

    @Test
    @DisplayName("FE13_ACC_UTC07 [Normal] - Mở khóa tài khoản người dùng (status='active') -> Cập nhật trạng thái thành công (Passed)")
    void UTC07_updateUserStatus_unlockUser_shouldUpdateStatusToActive() {
        when(userRepository.findById(20)).thenReturn(Optional.of(lockedUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UpdateUserStatusReq req = new UpdateUserStatusReq("active");
        UserResp resp = adminUserService.updateUserStatus(1, 20, req);

        assertNotNull(resp);
        assertEquals("ACTIVE", resp.accountStatus());
        verify(userRepository).save(lockedUser);
    }

    @Test
    @DisplayName("FE13_ACC_UTC08 [Abnormal] - Cập nhật trạng thái cho tài khoản không tồn tại -> Ném ResourceNotFoundException (Passed)")
    void UTC08_updateUserStatus_nonExistentId_shouldThrowResourceNotFound() {
        when(userRepository.findById(9999)).thenReturn(Optional.empty());
        UpdateUserStatusReq req = new UpdateUserStatusReq("locked");

        assertThrows(ResourceNotFoundException.class, () -> adminUserService.updateUserStatus(1, 9999, req));
    }

    @Test
    @DisplayName("FE13_ACC_UTC09 [Abnormal] - Quản trị viên cố tình tự khóa tài khoản của chính mình -> Ném BadRequestException (Passed)")
    void UTC09_updateUserStatus_adminSelfLock_shouldThrowBadRequest() {
        UpdateUserStatusReq req = new UpdateUserStatusReq("locked");

        assertThrows(BadRequestException.class, () -> adminUserService.updateUserStatus(1, 1, req));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("FE13_ACC_UTC10 [Boundary] - Lọc tài khoản với chuỗi status và search chứa nhiều khoảng trắng thừa -> Tự động trim() làm sạch (Passed)")
    void UTC10_getUsers_whitespaceFilters_shouldTrimCorrectly() {
        Page<User> mockPage = new PageImpl<>(List.of(normalUser), pageable, 1);
        when(userRepository.searchUsers("active", "farmer", pageable)).thenReturn(mockPage);

        PageResp<UserResp> result = adminUserService.getUsers("   ACTIVE   ", "   farmer   ", pageable);

        assertNotNull(result);
        assertEquals(1, result.totalElements());
        verify(userRepository).searchUsers(eq("active"), eq("farmer"), eq(pageable));
    }
}
