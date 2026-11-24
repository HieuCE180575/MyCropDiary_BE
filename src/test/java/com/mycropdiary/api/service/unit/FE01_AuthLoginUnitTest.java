package com.mycropdiary.api.service.unit;

import com.mycropdiary.api.dto.auth.AuthRespDTO;
import com.mycropdiary.api.dto.auth.LoginReqDTO;
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
        LoginReqDTO req = new LoginReqDTO("farmer@example.com", "Password123@");
        when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.of(activeUser));
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("mock-access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("mock-refresh-token");

        AuthRespDTO resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("mock-access-token", resp.accessToken());
        assertEquals("mock-refresh-token", resp.refreshToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("FE01_AUT_UTC02 [Abnormal] - Email không tồn tại trong hệ thống -> Báo lỗi BadRequestException (Passed)")
    void UTC02_login_nonExistentEmail_shouldThrowBadRequest() {
        LoginReqDTO req = new LoginReqDTO("notfound@example.com", "Password123@");
        when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("FE01_AUT_UTC03 [Abnormal] - Sai mật khẩu -> AuthenticationManager ném BadCredentialsException (Passed)")
    void UTC03_login_wrongPassword_shouldThrowBadCredentials() {
        LoginReqDTO req = new LoginReqDTO("farmer@example.com", "WrongPassword@");
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("FE01_AUT_UTC04 [Abnormal] - Tài khoản bị vô hiệu hóa -> Ném DisabledException (Passed)")
    void UTC04_login_disabledUser_shouldThrowDisabledException() {
        LoginReqDTO req = new LoginReqDTO("banned@example.com", "Password123@");
        doThrow(new DisabledException("User account is disabled"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(DisabledException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("FE01_AUT_UTC05 [Boundary] - Email để trống -> Ném ngoại lệ validation hoặc BadCredentials (Passed)")
    void UTC05_login_emptyEmail_shouldFailValidation() {
        LoginReqDTO req = new LoginReqDTO("", "Password123@");
        doThrow(new BadCredentialsException("Empty email"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("FE01_AUT_UTC06 [Boundary] - Mật khẩu có khoảng trắng đầu/cuối -> Tự động trim đồng bộ và đăng nhập thành công (Passed)")
    void UTC06_login_whitespacePassword_defectDF001() {
        // Verified fix for DF-001: AuthServiceImpl trims leading/trailing whitespace symmetrically
        LoginReqDTO req = new LoginReqDTO("farmer@example.com", " Password123@ ");
        when(userRepository.findByEmailIgnoreCase("farmer@example.com")).thenReturn(Optional.of(activeUser));
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("mock-access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("mock-refresh-token");

        AuthRespDTO resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("mock-access-token", resp.accessToken());
        verify(authenticationManager).authenticate(
                argThat(auth -> "Password123@".equals(auth.getCredentials()))
        );
    }

    @Test
    @DisplayName("FE01_AUT_UTC07 [Normal] - Email chữ hoa (Case-insensitive) -> Đăng nhập thành công (Passed)")
    void UTC07_login_uppercaseEmail_shouldNormalizeAndPass() {
        LoginReqDTO req = new LoginReqDTO("FARMER@EXAMPLE.COM", "Password123@");
        when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.of(activeUser));
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("mock-access-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("mock-refresh-token");

        AuthRespDTO resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("farmer@example.com", resp.email());
    }

    @Test
    @DisplayName("FE01_AUT_UTC08 [Abnormal] - SQL Injection payload trong email -> Chặn an toàn (Passed)")
    void UTC08_login_sqlInjectionPayload_shouldHandleSafely() {
        LoginReqDTO req = new LoginReqDTO("' OR 1=1 --", "Password123@");
        when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("FE01_AUT_UTC09 [Boundary] - Mật khẩu dài > 72 bytes -> Chặn an toàn bằng BadRequestException và Bean Validation (Passed)")
    void UTC09_login_passwordExceeds72Bytes_defectDF002() {
        // Verified fix for DF-002: LoginReq @Size(max=72) + service-level 72-byte UTF-8 check
        String longPass = "A".repeat(75) + "1@";
        LoginReqDTO req = new LoginReqDTO("farmer@example.com", longPass);

        assertThrows(BadRequestException.class, () -> authService.login(req));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    @DisplayName("FE01_AUT_UTC10 [Normal] - Đăng nhập với tài khoản Admin -> Trả về role admin (Passed)")
    void UTC10_login_adminUser_shouldIncludeAdminRole() {
        LoginReqDTO req = new LoginReqDTO("admin@example.com", "AdminPass123@");
        when(userRepository.findByEmailIgnoreCase(req.email())).thenReturn(Optional.of(adminUser));
        when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("admin-token");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("admin-refresh");

        AuthRespDTO resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("admin", resp.systemRole());
    }
}
