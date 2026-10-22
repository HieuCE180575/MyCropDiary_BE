package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.user.ChangePasswordReq;
import com.mycropdiary.api.dto.user.UpdateProfileReq;
import com.mycropdiary.api.dto.user.UserProfileResp;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

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
    @DisplayName("getProfile -> thành công trả về thông tin cá nhân")
    void getProfile_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileResp resp = userProfileService.getProfile(userId);

        assertNotNull(resp);
        assertEquals(userId, resp.userId());
        assertEquals("farmer@example.com", resp.email());
        assertEquals("Nguyen Van Farmer", resp.fullName());
    }

    @Test
    @DisplayName("getProfile -> ném lỗi nếu không tìm thấy người dùng")
    void getProfile_NotFound_ThrowsException() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userProfileService.getProfile(999));
    }

    @Test
    @DisplayName("updateProfile -> cập nhật họ tên và số điện thoại thành công")
    void updateProfile_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileReq req = new UpdateProfileReq("Le Van Tien", "0987654321", "https://avatar.url");
        UserProfileResp resp = userProfileService.updateProfile(userId, req);

        assertNotNull(resp);
        assertEquals("Le Van Tien", resp.fullName());
        assertEquals("0987654321", resp.phone());
        assertEquals("https://avatar.url", resp.avatarUrl());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("changePassword -> thành công khi mật khẩu hiện tại chính xác")
    void changePassword_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword@123", "hashed_old_password")).thenReturn(true);
        when(passwordEncoder.matches("NewPassword@456", "hashed_old_password")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword@456")).thenReturn("hashed_new_password");
        when(accountTokenRepository.findAllByUser_IdAndTokenTypeAndUsedAtIsNull(userId, "refresh"))
                .thenReturn(Collections.emptyList());

        ChangePasswordReq req = new ChangePasswordReq("OldPassword@123", "NewPassword@456", "NewPassword@456");
        assertDoesNotThrow(() -> userProfileService.changePassword(userId, req));

        assertEquals("hashed_new_password", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("changePassword -> lỗi khi mật khẩu xác nhận không khớp")
    void changePassword_ConfirmPasswordMismatch_ThrowsBadRequest() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ChangePasswordReq req = new ChangePasswordReq("OldPassword@123", "NewPassword@456", "WrongConfirmPassword");
        assertThrows(BadRequestException.class, () -> userProfileService.changePassword(userId, req));
    }

    @Test
    @DisplayName("changePassword -> lỗi khi mật khẩu hiện tại sai")
    void changePassword_WrongCurrentPassword_ThrowsBadRequest() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongOldPassword", "hashed_old_password")).thenReturn(false);

        ChangePasswordReq req = new ChangePasswordReq("WrongOldPassword", "NewPassword@456", "NewPassword@456");
        assertThrows(BadRequestException.class, () -> userProfileService.changePassword(userId, req));
    }

    @Test
    @DisplayName("changePassword -> lỗi khi mật khẩu mới trùng mật khẩu cũ")
    void changePassword_SamePassword_ThrowsBadRequest() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPassword@123", "hashed_old_password")).thenReturn(true);
        when(passwordEncoder.matches("OldPassword@123", "hashed_old_password")).thenReturn(true);

        ChangePasswordReq req = new ChangePasswordReq("OldPassword@123", "OldPassword@123", "OldPassword@123");
        assertThrows(BadRequestException.class, () -> userProfileService.changePassword(userId, req));
    }
}
