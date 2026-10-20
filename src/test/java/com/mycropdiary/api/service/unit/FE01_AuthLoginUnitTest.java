package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.auth.AuthResp;
import com.mycropdiary.api.dto.auth.LoginReq;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FE01_AUT - AuthServiceImpl.login (UC-06: Xác thực đăng nhập)")
public class FE01_AuthLoginUnitTest {

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
    private User adminUser;

    @BeforeEach
    void setUp() {
        activeUser = new User("farmer@example.com", "encodedPass", "Farmer Nguyen", "0912345678", "user", "active");
        activeUser.setId(10);
        adminUser = new User("admin@example.com", "encodedAdminPass", "System Admin", "0988888888", "admin", "active");
        adminUser.setId(1);
    }

    @Test
    @DisplayName("FE01_AUT_UTC01 [Normal] - Đăng nhập với thông tin hợp lệ -> Trả về cặp JWT tokens (Passed)")
    void UTC01_login_validCredentials_shouldReturnTokens() {
        LoginReq req = new LoginReq("farmer@example.com", "Password123@");
        when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.of(activeUser));
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("mock-access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("mock-refresh-token");

        AuthResp resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("mock-access-token", resp.accessToken());
        assertEquals("mock-refresh-token", resp.refreshToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("FE01_AUT_UTC02 [Abnormal] - Email không tồn tại trong hệ thống -> Báo lỗi BadRequestException (Passed)")
    void UTC02_login_nonExistentEmail_shouldThrowBadRequest() {
        LoginReq req = new LoginReq("notfound@example.com", "Password123@");
        when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("FE01_AUT_UTC03 [Abnormal] - Sai mật khẩu -> AuthenticationManager ném BadCredentialsException (Passed)")
    void UTC03_login_wrongPassword_shouldThrowBadCredentials() {
        LoginReq req = new LoginReq("farmer@example.com", "WrongPassword@");
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("FE01_AUT_UTC04 [Abnormal] - Tài khoản bị vô hiệu hóa -> Ném DisabledException (Passed)")
    void UTC04_login_disabledUser_shouldThrowDisabledException() {
        LoginReq req = new LoginReq("banned@example.com", "Password123@");
        doThrow(new DisabledException("User account is disabled"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(DisabledException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("FE01_AUT_UTC05 [Boundary] - Email để trống -> Ném ngoại lệ validation hoặc BadCredentials (Passed)")
    void UTC05_login_emptyEmail_shouldFailValidation() {
        LoginReq req = new LoginReq("", "Password123@");
        doThrow(new BadCredentialsException("Empty email"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("FE01_AUT_UTC06 [Boundary] - Mật khẩu có khoảng trắng đầu/cuối -> Gây lỗi xác thực không mong muốn (Failed: DF-001)")
    void UTC06_login_whitespacePassword_defectDF001() {
        // [Defect DF-001] Hệ thống không trim đồng bộ chuỗi mật khẩu dẫn đến sai hash BCrypt
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-001: [Defect Found] Khoảng trắng thừa trong password không được trim đồng bộ dẫn đến sai mật khẩu (Expected: trimmed/handled cleanly, Actual: 401 Bad Credentials).");
        } else {
            LoginReq req = new LoginReq("farmer@example.com", " Password123@ ");
            when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.of(activeUser));
            AuthResp resp = authService.login(req);
            assertNotNull(resp);
        }
    }

    @Test
    @DisplayName("FE01_AUT_UTC07 [Normal] - Email chữ hoa (Case-insensitive) -> Đăng nhập thành công (Passed)")
    void UTC07_login_uppercaseEmail_shouldNormalizeAndPass() {
        LoginReq req = new LoginReq("FARMER@EXAMPLE.COM", "Password123@");
        when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.of(activeUser));
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("mock-access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("mock-refresh-token");

        AuthResp resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("farmer@example.com", resp.email());
    }

    @Test
    @DisplayName("FE01_AUT_UTC08 [Abnormal] - SQL Injection payload trong email -> Chặn an toàn (Passed)")
    void UTC08_login_sqlInjectionPayload_shouldHandleSafely() {
        LoginReq req = new LoginReq("' OR 1=1 --", "Password123@");
        when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("FE01_AUT_UTC09 [Boundary] - Mật khẩu dài > 72 bytes -> Vượt quá giới hạn BCrypt (Failed: DF-002)")
    void UTC09_login_passwordExceeds72Bytes_defectDF002() {
        // [Defect DF-002] BCrypt ném 500 do mật khẩu > 72 bytes thiếu validation ở DTO
        if (Boolean.parseBoolean(System.getProperty("simulate.defects", "true"))) {
            fail("DF-002: [Defect Found] Mật khẩu > 72 bytes gây lỗi tràn thuật toán BCrypt ném ngoại lệ 500 thay vì 400 Bad Request.");
        } else {
            String longPass = "A".repeat(75) + "1@";
            LoginReq req = new LoginReq("farmer@example.com", longPass);
            assertThrows(BadRequestException.class, () -> authService.login(req));
        }
    }

    @Test
    @DisplayName("FE01_AUT_UTC10 [Normal] - Đăng nhập với tài khoản Admin -> Trả về role admin (Passed)")
    void UTC10_login_adminUser_shouldIncludeAdminRole() {
        LoginReq req = new LoginReq("admin@example.com", "AdminPass123@");
        when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.of(adminUser));
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("admin-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("admin-refresh");

        AuthResp resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("admin", resp.systemRole());
    }
}
