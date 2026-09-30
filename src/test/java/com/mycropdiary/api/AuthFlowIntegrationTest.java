package com.mycropdiary.api;

import com.mycropdiary.api.dto.auth.AuthResponse;
import com.mycropdiary.api.dto.auth.RegisterRequest;
import com.mycropdiary.api.dto.auth.VerifyOtpRequest;
import com.mycropdiary.api.entity.AccountToken;
import com.mycropdiary.api.entity.AppUser;
import com.mycropdiary.api.repository.AccountTokenRepository;
import com.mycropdiary.api.repository.AppUserRepository;
import com.mycropdiary.api.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.test.context.ActiveProfiles;

// [AI_CHANGE] Root cause: Kiểm tra lỗi BCrypt 72 bytes khi verify OTP và issue tokens
// [AI_CHANGE] Mechanism: Chạy toàn bộ flow đăng ký -> tìm OTP -> xác thực OTP -> cấp Access & Refresh tokens
@SpringBootTest
@ActiveProfiles("local")
class AuthFlowIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AccountTokenRepository accountTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Test
    @DisplayName("Đăng ký tài khoản và xác thực OTP thành công, không gặp lỗi BCrypt 72 bytes")
    void testRegisterAndVerifyOtpSuccessfully() {
        String testEmail = "testuser_otp@fpt.edu.vn";

        // 1. Đăng ký
        RegisterRequest registerReq = new RegisterRequest(
                "Test User OTP",
                testEmail,
                "0987654321",
                "StrongPassword123@"
        );
        authService.register(registerReq);

        // 2. Tìm user và OTP trong CSDL
        AppUser user = appUserRepository.findByEmailIgnoreCase(testEmail).orElseThrow();
        assertThat(user.getAccountStatus()).isEqualTo("PENDING");

        AccountToken otpEntity = accountTokenRepository
                .findFirstByUser_IdAndTokenTypeAndUsedAtIsNullAndRevokedAtIsNullOrderByCreatedAtDesc(user.getId(), "EMAIL_OTP")
                .orElseThrow();

        // 3. Giả lập người dùng nhập mã OTP đúng
        // Vì OTP entity lưu BCrypt hash, ta tìm mã 6 số khớp hoặc test trực tiếp với OTP
        // Ở đây để test issueTokens không bị lỗi 72 bytes:
        // Đổi OTP token thành hash của "123456"
        AccountToken newOtp = AccountToken.createOtp(user, passwordEncoder.encode("123456"), otpEntity.getExpiresAt());
        otpEntity.revoke();
        accountTokenRepository.save(otpEntity);
        accountTokenRepository.save(newOtp);

        // 4. Verify OTP
        VerifyOtpRequest verifyReq = new VerifyOtpRequest(testEmail, "123456");
        AuthResponse authResponse = authService.verifyOtp(verifyReq);

        // 5. Kiểm tra kết quả
        assertThat(authResponse).isNotNull();
        assertThat(authResponse.accessToken()).isNotBlank();
        assertThat(authResponse.refreshToken()).isNotBlank();
        assertThat(authResponse.email()).isEqualTo(testEmail);

        AppUser activatedUser = appUserRepository.findByEmailIgnoreCase(testEmail).orElseThrow();
        assertThat(activatedUser.getAccountStatus()).isEqualTo("ACTIVE");
    }
}
