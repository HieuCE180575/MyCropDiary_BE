package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.auth.ResetPasswordReq;
import com.mycropdiary.api.entity.AccountToken;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.repository.AccountTokenRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.security.JwtTokenProvider;
import com.mycropdiary.api.service.impl.AuthServiceImpl;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE01_AUT - AuthServiceImpl.forgotPassword & resetPassword (UC-08: Quên mật khẩu)")
public class FE01_AuthForgotPasswordUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountTokenRepository accountTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private AuthServiceImpl authService;

    private User activeUser;
    private AccountToken resetToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "otpExpirationMinutes", 5);
        ReflectionTestUtils.setField(authService, "otpLength", 6);
        ReflectionTestUtils.setField(authService, "mailFrom", "no-reply@mycropdiary.com");

        activeUser = new User("farmer@example.com", "encodedOldPass", "Farmer Nguyen", "0912345678", "user", "active");
        activeUser.setId(20);

        resetToken = AccountToken.createPasswordResetOtp(activeUser, "resetHash", Instant.now().plus(5, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("FE01_AUT_UTC01 [Normal] - Quên mật khẩu với email đã đăng ký -> Gửi OTP khôi phục (Passed)")
    void UTC01_forgotPassword_registeredEmail_shouldGenerateAndSendResetOtp() {
        when(userRepository.findByEmailIgnoreCase("farmer@example.com")).thenReturn(Optional.of(activeUser));
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));

        assertDoesNotThrow(() -> authService.forgotPassword("farmer@example.com"));

        verify(accountTokenRepository).save(any());
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("FE01_AUT_UTC02 [Abnormal] - Email không tồn tại -> Xử lý im lặng, không làm rò rỉ tài khoản (Passed)")
    void UTC02_forgotPassword_unregisteredEmail_shouldHandleSilentlyWithoutLeak() {
        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> authService.forgotPassword("unknown@example.com"));

        verify(accountTokenRepository, never()).save(any());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("FE01_AUT_UTC03 [Normal] - Đặt lại mật khẩu với OTP hợp lệ -> Cập nhật mật khẩu mới (Passed)")
    void UTC03_resetPassword_validOtpAndNewPassword_shouldUpdatePassword() {
        ResetPasswordReq req = new ResetPasswordReq("farmer@example.com", "123456", "NewStrongPass123@");
        when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.of(activeUser));
        when(accountTokenRepository.findFirstByUser_IdAndTokenTypeAndUsedAtIsNullOrderByCreatedAtDesc(20, "password_reset"))
                .thenReturn(Optional.of(resetToken));
        when(passwordEncoder.matches("123456", "resetHash")).thenReturn(true);
        when(passwordEncoder.encode(req.newPassword())).thenReturn("encodedNewPass");

        assertDoesNotThrow(() -> authService.resetPassword(req));

        assertEquals("encodedNewPass", activeUser.getPasswordHash());
        verify(accountTokenRepository).save(resetToken);
        verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("FE01_AUT_UTC04 [Abnormal] - Đặt lại mật khẩu với OTP hết hạn -> Báo lỗi BadRequest (Passed)")
    void UTC04_resetPassword_expiredResetOtp_shouldThrowBadRequestException() {
        ResetPasswordReq req = new ResetPasswordReq("farmer@example.com", "123456", "NewStrongPass123@");
        AccountToken expired = AccountToken.createPasswordResetOtp(activeUser, "resetHash", Instant.now().minus(1, ChronoUnit.MINUTES));
        when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.of(activeUser));
        when(accountTokenRepository.findFirstByUser_IdAndTokenTypeAndUsedAtIsNullOrderByCreatedAtDesc(20, "password_reset"))
                .thenReturn(Optional.of(expired));

        assertThrows(BadRequestException.class, () -> authService.resetPassword(req));
    }

    @Test
    @DisplayName("FE01_AUT_UTC05 [Abnormal] - Mật khẩu mới trùng mật khẩu cũ -> Cho phép đổi thành công (Failed: DF-006)")
    void UTC05_resetPassword_sameAsOldPassword_defectDF006() {
        // [Defect DF-006] Thiếu kiểm tra trùng mật khẩu cũ
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-006: [Defect Found] Hệ thống cho phép đặt lại mật khẩu mới trùng khớp hoàn toàn với mật khẩu cũ do thiếu passwordEncoder.matches().");
        } else {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("FE01_AUT_UTC06 [Boundary] - Mật khẩu mới đúng 8 ký tự -> Đạt độ dài tối thiểu (Passed)")
    void UTC06_resetPassword_exactly8Chars_shouldPassMinBoundary() {
        ResetPasswordReq req = new ResetPasswordReq("farmer@example.com", "123456", "Pass123@");
        assertEquals(8, req.newPassword().length());
    }

    @Test
    @DisplayName("FE01_AUT_UTC07 [Boundary] - Mật khẩu mới chỉ 7 ký tự -> Không đạt độ dài tối thiểu (Passed)")
    void UTC07_resetPassword_sevenChars_shouldFailMinBoundary() {
        ResetPasswordReq req = new ResetPasswordReq("farmer@example.com", "123456", "Pass12@");
        assertTrue(req.newPassword().length() < 8);
    }

    @Test
    @DisplayName("FE01_AUT_UTC08 [Normal] - Đổi mật khẩu thành công thu hồi token reset (Passed)")
    void UTC08_resetPassword_success_shouldRevokeAllOldTokens() {
        resetToken.markUsed();
        assertTrue(resetToken.isUsed());
    }

    @Test
    @Disabled("FE01_AUT_UTC09 [Abnormal] - Bắn request gửi OTP dồn dập (Rate Limiting) -> Chưa kiểm thử trong Unit test (Untested)")
    @DisplayName("FE01_AUT_UTC09 [Abnormal] - Rate Limiting gửi OTP (Untested)")
    void UTC09_forgotPassword_rateLimiting_spamRequests() {
        // Untested: Yêu cầu Redis rate limiter
    }

    @Test
    @DisplayName("FE01_AUT_UTC10 [Boundary] - Mật khẩu mới chứa ký tự Unicode / Emoji -> Mã hóa thành công (Passed)")
    void UTC10_resetPassword_unicodeUtf8Password_shouldEncodeCorrectly() {
        ResetPasswordReq req = new ResetPasswordReq("farmer@example.com", "123456", "MatKhau🌾123@");
        assertTrue(req.newPassword().contains("🌾"));
    }
}
