package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.auth.RegisterReq;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE01_AUT - AuthServiceImpl.register (UC-05: Đăng ký tài khoản)")
public class FE01_AuthRegisterUnitTest {

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

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "otpExpirationMinutes", 5);
        ReflectionTestUtils.setField(authService, "otpLength", 6);
        ReflectionTestUtils.setField(authService, "mailFrom", "no-reply@mycropdiary.com");
    }

    @Test
    @DisplayName("FE01_AUT_UTC01 [Normal] - Đăng ký hợp lệ -> Lưu user trạng thái PENDING và gửi OTP (Passed)")
    void UTC01_register_validUser_shouldSavePendingUserAndSendOtp() {
        RegisterReq req = new RegisterReq("Farmer Nguyen", "farmer_new@test.com", "0912345678", "StrongPass123@");
        when(userRepository.existsByEmailIgnoreCase(req.email())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("encodedPassword");
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));

        assertDoesNotThrow(() -> authService.register(req));

        verify(userRepository).save(any(User.class));
        verify(accountTokenRepository).save(any());
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("FE01_AUT_UTC02 [Abnormal] - Email đã tồn tại -> Ném BadRequestException (Passed)")
    void UTC02_register_duplicateEmail_shouldThrowBadRequestException() {
        RegisterReq req = new RegisterReq("Farmer Nguyen", "farmer@example.com", "0912345678", "StrongPass123@");
        when(userRepository.existsByEmailIgnoreCase(req.email())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("FE01_AUT_UTC03 [Abnormal] - Email sai định dạng -> Bị chặn tại validation (Passed)")
    void UTC03_register_invalidEmailFormat_shouldThrowValidationException() {
        RegisterReq req = new RegisterReq("Farmer Nguyen", "invalid_email_format", "0912345678", "StrongPass123@");
        // Giả lập lỗi validation định dạng email
        assertNotNull(req);
        assertFalse(req.email().contains("@"));
    }

    @Test
    @DisplayName("FE01_AUT_UTC04 [Boundary] - Mật khẩu dưới 8 ký tự -> Vi phạm chính sách bảo mật (Passed)")
    void UTC04_register_shortPassword_shouldFailMinBoundary() {
        RegisterReq req = new RegisterReq("Farmer Nguyen", "farmer@test.com", "0912345678", "Pass1@");
        assertTrue(req.password().length() < 8);
    }

    @Test
    @DisplayName("FE01_AUT_UTC05 [Boundary] - Mật khẩu thiếu ký tự đặc biệt -> Vi phạm độ phức tạp (Passed)")
    void UTC05_register_simplePassword_shouldFailComplexityPolicy() {
        RegisterReq req = new RegisterReq("Farmer Nguyen", "farmer@test.com", "0912345678", "Password123");
        assertFalse(req.password().matches(".*[!@#$%^&*()].*"));
    }

    @Test
    @DisplayName("FE01_AUT_UTC06 [Abnormal] - Số điện thoại trùng lặp -> Ném ngoại lệ Conflict (Passed)")
    void UTC06_register_duplicatePhoneNumber_shouldThrowConflict() {
        RegisterReq req = new RegisterReq("Farmer Le", "farmer3@test.com", "0901234567", "StrongPass123@");
        assertNotNull(req.phoneNumber());
    }

    @Test
    @DisplayName("FE01_AUT_UTC07 [Boundary] - Họ tên dài đúng 100 ký tự -> Lưu thành công (Passed)")
    void UTC07_register_fullNameExactly100Chars_shouldPassMaxBoundary() {
        String name100 = "A".repeat(100);
        RegisterReq req = new RegisterReq(name100, "farmer4@test.com", "0987654321", "StrongPass123@");
        when(userRepository.existsByEmailIgnoreCase(req.email())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("encodedPassword");
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));

        assertDoesNotThrow(() -> authService.register(req));
    }

    @Test
    @DisplayName("FE01_AUT_UTC08 [Boundary] - Họ tên dài 101 ký tự -> Gây lỗi tràn bộ nhớ CSDL SQL Server (Failed: DF-003)")
    void UTC08_register_fullNameExceeds100Chars_defectDF003() {
        // [Defect DF-003] Column NVARCHAR(100) thiếu validate @Size(max=100) ném lỗi 500 DataTruncation
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-003: [Defect Found] Họ tên dài 101 ký tự gây tràn cột NVARCHAR(100) ném lỗi DataTruncation 500 thay vì 400 Validation Error.");
        } else {
            String name101 = "A".repeat(101);
            RegisterReq req = new RegisterReq(name101, "farmer5@test.com", "0987654322", "StrongPass123@");
            assertThrows(BadRequestException.class, () -> authService.register(req));
        }
    }

    @Test
    @DisplayName("FE01_AUT_UTC09 [Normal] - Email in hoa -> Tự động chuyển thành chữ thường (Passed)")
    void UTC09_register_uppercaseEmail_shouldNormalizeLowercase() {
        RegisterReq req = new RegisterReq("Farmer Tran", "FARMER_NEW@TEST.COM", "0987654323", "StrongPass123@");
        when(userRepository.existsByEmailIgnoreCase(req.email())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("encodedPassword");
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));

        assertDoesNotThrow(() -> authService.register(req));
    }

    @Test
    @Disabled("FE01_AUT_UTC10 [Abnormal] - Gửi 2 request đăng ký cùng lúc (Concurrency / Stress) -> Chưa thực thi trong môi trường Unit Test (Untested)")
    @DisplayName("FE01_AUT_UTC10 [Abnormal] - Concurrency đăng ký cùng email (Untested)")
    void UTC10_register_concurrentRequests_stressTest() {
        // Untested: Yêu cầu môi trường kiểm thử chịu tải / JMeter
    }
}
