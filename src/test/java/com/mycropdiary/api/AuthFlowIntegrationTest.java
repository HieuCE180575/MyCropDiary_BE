package com.mycropdiary.api;

import com.mycropdiary.api.dto.auth.AuthResp;
import com.mycropdiary.api.dto.auth.RegisterReq;
import com.mycropdiary.api.dto.auth.VerifyOtpReq;
import com.mycropdiary.api.entity.AccountToken;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.repository.AccountTokenRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;

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
    private UserRepository userRepository;

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
        RegisterReq registerReq = new RegisterReq(
                "Test User OTP",
                testEmail,
                "0987654321",
                "StrongPassword123@"
        );
        authService.register(registerReq);

        // 2. Tìm user và OTP trong CSDL
        User user = userRepository.findByEmailIgnoreCase(testEmail).orElseThrow();
        assertThat(user.getAccountStatus()).isEqualToIgnoringCase("pending");

        AccountToken otpEntity = accountTokenRepository
                .findFirstByUser_IdAndTokenTypeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), "email_verification")
                .orElseThrow();

        // 3. Giả lập người dùng nhập mã OTP đúng
        AccountToken newOtp = AccountToken.createOtp(user, passwordEncoder.encode("123456"), otpEntity.getExpiresAt());
        otpEntity.setUsedAt(Instant.now());
        accountTokenRepository.save(otpEntity);
        accountTokenRepository.save(newOtp);

        // 4. Verify OTP
        VerifyOtpReq verifyReq = new VerifyOtpReq(testEmail, "123456");
        AuthResp authResponse = authService.verifyOtp(verifyReq);

        // 5. Kiểm tra kết quả
        assertThat(authResponse).isNotNull();
        assertThat(authResponse.accessToken()).isNotBlank();
        assertThat(authResponse.refreshToken()).isNotBlank();
        assertThat(authResponse.email()).isEqualTo(testEmail);

        User activatedUser = userRepository.findByEmailIgnoreCase(testEmail).orElseThrow();
        assertThat(activatedUser.getAccountStatus()).isEqualToIgnoringCase("active");
    }
}
