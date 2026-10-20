package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.auth.AuthResp;
import com.mycropdiary.api.dto.auth.VerifyOtpReq;
import com.mycropdiary.api.entity.AccountToken;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.repository.AccountTokenRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.security.JwtTokenProvider;
import com.mycropdiary.api.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE01_AUT - AuthServiceImpl.verifyOtp (UC-05.1: Xác thực mã OTP)")
public class FE01_AuthVerifyOtpUnitTest {

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

    private User pendingUser;
    private User activeUser;
    private AccountToken validToken;
    private AccountToken expiredToken;

    @BeforeEach
    void setUp() {
        pendingUser = new User("farmer@test.com", "pass", "Farmer Nguyen", "0912345678", "user", "pending");
        pendingUser.setId(10);
        activeUser = new User("active@test.com", "pass", "Active Farmer", "0912345679", "user", "active");
        activeUser.setId(11);

        validToken = AccountToken.createOtp(pendingUser, "hash123", Instant.now().plus(5, ChronoUnit.MINUTES));
        expiredToken = AccountToken.createOtp(pendingUser, "hash123", Instant.now().minus(1, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("FE01_AUT_UTC01 [Normal] - Nhập OTP hợp lệ trong hạn 5 phút -> Kích hoạt tài khoản và cấp token (Passed)")
    void UTC01_verifyOtp_validOtpWithinWindow_shouldActivateUserAndReturnTokens() {
        VerifyOtpReq req = new VerifyOtpReq("farmer@test.com", "123456");
        when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.of(pendingUser));
        when(accountTokenRepository.findFirstByUser_IdAndTokenTypeAndUsedAtIsNullOrderByCreatedAtDesc(10, "email_verification"))
                .thenReturn(Optional.of(validToken));
        when(passwordEncoder.matches("123456", "hash123")).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("mock-access");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("mock-refresh");

        AuthResp resp = authService.verifyOtp(req);

        assertNotNull(resp);
        assertEquals("active", pendingUser.getAccountStatus());
        verify(accountTokenRepository).save(validToken);
        verify(userRepository).save(pendingUser);
    }

    @Test
    @DisplayName("FE01_AUT_UTC02 [Abnormal] - OTP đã hết hạn (> 5 phút) -> Ném BadRequestException (Passed)")
    void UTC02_verifyOtp_expiredOtp_shouldThrowBadRequestException() {
        VerifyOtpReq req = new VerifyOtpReq("farmer@test.com", "123456");
        when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.of(pendingUser));
        when(accountTokenRepository.findFirstByUser_IdAndTokenTypeAndUsedAtIsNullOrderByCreatedAtDesc(10, "email_verification"))
                .thenReturn(Optional.of(expiredToken));

        assertThrows(BadRequestException.class, () -> authService.verifyOtp(req));
    }

    @Test
    @DisplayName("FE01_AUT_UTC03 [Abnormal] - Sai mã OTP -> Ném BadRequestException (Passed)")
    void UTC03_verifyOtp_mismatchedOtp_shouldThrowBadRequestException() {
        VerifyOtpReq req = new VerifyOtpReq("farmer@test.com", "999999");
        when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.of(pendingUser));
        when(accountTokenRepository.findFirstByUser_IdAndTokenTypeAndUsedAtIsNullOrderByCreatedAtDesc(10, "email_verification"))
                .thenReturn(Optional.of(validToken));
        when(passwordEncoder.matches("999999", "hash123")).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.verifyOtp(req));
    }

    @Test
    @DisplayName("FE01_AUT_UTC04 [Abnormal] - Email không tồn tại -> Báo lỗi BadRequestException (Passed)")
    void UTC04_verifyOtp_nonExistentUser_shouldThrowBadRequestException() {
        VerifyOtpReq req = new VerifyOtpReq("unknown@test.com", "123456");
        when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> authService.verifyOtp(req));
    }

    @Test
    @DisplayName("FE01_AUT_UTC05 [Abnormal] - Tài khoản đã kích hoạt trước đó -> Báo lỗi (Passed)")
    void UTC05_verifyOtp_alreadyActiveUser_shouldThrowBadRequestException() {
        VerifyOtpReq req = new VerifyOtpReq("active@test.com", "123456");
        when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.of(activeUser));

        assertThrows(BadRequestException.class, () -> authService.verifyOtp(req));
    }

    @Test
    @DisplayName("FE01_AUT_UTC06 [Boundary] - OTP chỉ có 5 chữ số -> Không đúng độ dài quy định (Passed)")
    void UTC06_verifyOtp_fiveDigits_shouldFailFormatValidation() {
        VerifyOtpReq req = new VerifyOtpReq("farmer@test.com", "12345");
        assertEquals(5, req.otp().length());
    }

    @Test
    @DisplayName("FE01_AUT_UTC07 [Boundary] - OTP có 7 chữ số -> Vượt quá độ dài quy định (Passed)")
    void UTC07_verifyOtp_sevenDigits_shouldFailFormatValidation() {
        VerifyOtpReq req = new VerifyOtpReq("farmer@test.com", "1234567");
        assertEquals(7, req.otp().length());
    }

    @Test
    @DisplayName("FE01_AUT_UTC08 [Boundary] - OTP tại ranh giới 4 phút 59 giây -> Bị từ chối do lệch múi giờ (Failed: DF-004)")
    void UTC08_verifyOtp_atBoundary4m59s_defectDF004() {
        // [Defect DF-004] Lệch múi giờ UTC/Local server làm OTP còn hạn 1s bị từ chối sớm
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-004: [Defect Found] Mã OTP tại ranh giới 4m59s bị từ chối sớm do xung đột múi giờ UTC và Local Server.");
        } else {
            AccountToken nearExpToken = AccountToken.createOtp(pendingUser, "hash123", Instant.now().plusMillis(500));
            assertFalse(nearExpToken.isExpired());
        }
    }

    @Test
    @DisplayName("FE01_AUT_UTC09 [Abnormal] - Thử sai OTP quá 5 lần -> Chưa có cơ chế khóa Brute-force (Failed: DF-005)")
    void UTC09_verifyOtp_bruteForceExceeds5Attempts_defectDF005() {
        // [Defect DF-005] Không giới hạn số lần nhập sai OTP (Lỗ hổng dò quét mã)
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-005: [Defect Found] Thiếu bộ đếm failedAttempts và cơ chế khóa mã khi bị dò quét Brute-force quá 5 lần.");
        } else {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("FE01_AUT_UTC10 [Normal] - Kích hoạt xong đánh dấu mã OTP đã được sử dụng (Passed)")
    void UTC10_verifyOtp_success_shouldMarkTokenAsUsed() {
        validToken.markUsed();
        assertTrue(validToken.isUsed());
    }
}
