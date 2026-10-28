package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.user.ChangePasswordReq;
import com.mycropdiary.api.dto.user.UpdateProfileReq;
import com.mycropdiary.api.dto.user.UserProfileResp;
import com.mycropdiary.api.entity.AccountToken;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.AccountTokenRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.service.impl.UserProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE02_PRO - UserProfileServiceImpl (UC-09: Quản lý thông tin cá nhân & Đổi mật khẩu)")
public class FE02_UserProfileUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountTokenRepository accountTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    private User user;
    private final Integer userId = 10;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(userId);
        user.setFullName("Nguyen Van Farmer");
        user.setEmail("farmer@example.com");
        user.setPhone("0912345678");
        user.setPasswordHash("hashed_old_password");
        user.setSystemRole("user");
        user.setAccountStatus("active");
    }

    @Test
    @DisplayName("FE02_PRO_UTC01 [Normal] - Xem hồ sơ người dùng hợp lệ -> Trả về UserProfileResp đầy đủ (Passed)")
    void UTC01_getProfile_validUserId_shouldReturnUserProfile() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileResp resp = userProfileService.getProfile(userId);

        assertNotNull(resp);
        assertEquals(userId, resp.userId());
        assertEquals("farmer@example.com", resp.email());
        assertEquals("Nguyen Van Farmer", resp.fullName());
        assertEquals("0912345678", resp.phone());
        assertEquals("USER", resp.systemRole());
        assertEquals("ACTIVE", resp.accountStatus());
    }

    @Test
    @DisplayName("FE02_PRO_UTC02 [Abnormal] - Xem hồ sơ người dùng với ID không tồn tại (999) -> Ném ResourceNotFoundException (Passed)")
    void UTC02_getProfile_nonExistentUserId_shouldThrowResourceNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userProfileService.getProfile(999));
    }

    @Test
    @DisplayName("FE02_PRO_UTC03 [Normal] - Cập nhật họ tên và số điện thoại hợp lệ -> Cập nhật thành công và trả về DTO (Passed)")
    void UTC03_updateProfile_validData_shouldUpdateAndReturnResp() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UpdateProfileReq req = new UpdateProfileReq("Le Van Tien", "0987654321", "https://image.url/avatar.png");
        UserProfileResp resp = userProfileService.updateProfile(userId, req);

        assertNotNull(resp);
        assertEquals("Le Van Tien", resp.fullName());
        assertEquals("0987654321", resp.phone());
        assertEquals("https://image.url/avatar.png", resp.avatarUrl());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("FE02_PRO_UTC04 [Abnormal] - Cập nhật hồ sơ với ID không tồn tại -> Ném ResourceNotFoundException (Passed)")
    void UTC04_updateProfile_nonExistentUserId_shouldThrowResourceNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        UpdateProfileReq req = new UpdateProfileReq("Le Van Tien", "0987654321", null);

        assertThrows(ResourceNotFoundException.class, () -> userProfileService.updateProfile(999, req));
    }

    @Test
    @DisplayName("FE02_PRO_UTC05 [Boundary] - Cập nhật hồ sơ với chuỗi phone và avatar rỗng -> Tự động chuyển về null (Passed)")
    void UTC05_updateProfile_emptyPhoneAndAvatar_shouldSetNull() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UpdateProfileReq req = new UpdateProfileReq("Farmer Tran", "   ", "   ");
        UserProfileResp resp = userProfileService.updateProfile(userId, req);

        assertNotNull(resp);
        assertNull(resp.phone());
        assertNull(resp.avatarUrl());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("FE02_PRO_UTC06 [Normal] - Đổi mật khẩu thành công khi mật khẩu cũ đúng và xác nhận khớp -> Mã hóa và thu hồi token (Passed)")
    void UTC06_changePassword_validRequest_shouldEncodePasswordAndRevokeTokens() {
        AccountToken oldToken = new AccountToken();
        oldToken.setId(101);
        oldToken.setTokenType("refresh");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@123", "hashed_old_password")).thenReturn(true);
        when(passwordEncoder.matches("NewPass@456", "hashed_old_password")).thenReturn(false);
        when(passwordEncoder.encode("NewPass@456")).thenReturn("hashed_new_password");
        when(accountTokenRepository.findAllByUser_IdAndTokenTypeAndUsedAtIsNull(userId, "refresh"))
                .thenReturn(List.of(oldToken));

        ChangePasswordReq req = new ChangePasswordReq("OldPass@123", "NewPass@456", "NewPass@456");
        assertDoesNotThrow(() -> userProfileService.changePassword(userId, req));

        assertEquals("hashed_new_password", user.getPasswordHash());
        verify(userRepository).save(user);
        verify(accountTokenRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("FE02_PRO_UTC07 [Abnormal] - Đổi mật khẩu với mật khẩu xác nhận không khớp -> Ném BadRequestException (Passed)")
    void UTC07_changePassword_confirmMismatch_shouldThrowBadRequest() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ChangePasswordReq req = new ChangePasswordReq("OldPass@123", "NewPass@456", "WrongConfirmPass@456");
        assertThrows(BadRequestException.class, () -> userProfileService.changePassword(userId, req));
    }

    @Test
    @DisplayName("FE02_PRO_UTC08 [Abnormal] - Đổi mật khẩu với mật khẩu hiện tại không chính xác -> Ném BadRequestException (Passed)")
    void UTC08_changePassword_wrongCurrentPassword_shouldThrowBadRequest() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongOldPass@123", "hashed_old_password")).thenReturn(false);

        ChangePasswordReq req = new ChangePasswordReq("WrongOldPass@123", "NewPass@456", "NewPass@456");
        assertThrows(BadRequestException.class, () -> userProfileService.changePassword(userId, req));
    }

    @Test
    @DisplayName("FE02_PRO_UTC09 [Abnormal] - Đổi mật khẩu mới trùng lặp hoàn toàn với mật khẩu cũ -> Ném BadRequestException (Passed)")
    void UTC09_changePassword_samePassword_shouldThrowBadRequest() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass@123", "hashed_old_password")).thenReturn(true);

        ChangePasswordReq req = new ChangePasswordReq("OldPass@123", "OldPass@123", "OldPass@123");
        assertThrows(BadRequestException.class, () -> userProfileService.changePassword(userId, req));
    }

    @Test
    @DisplayName("FE02_PRO_UTC10 [Boundary] - Cập nhật họ tên có khoảng trắng thừa đầu và cuối -> Tự động trim() làm sạch dữ liệu (Passed)")
    void UTC10_updateProfile_fullNameWithWhitespace_shouldTrimCleanly() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UpdateProfileReq req = new UpdateProfileReq("   Nguyen Van Tien   ", "0912345678", null);
        UserProfileResp resp = userProfileService.updateProfile(userId, req);

        assertNotNull(resp);
        assertEquals("Nguyen Van Tien", resp.fullName());
        verify(userRepository).save(user);
    }
}
