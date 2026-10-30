package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.auth.*;
import com.mycropdiary.api.entity.AccountToken;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.repository.AccountTokenRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.security.JwtTokenProvider;
import com.mycropdiary.api.service.AuthService;
import com.mycropdiary.api.util.EmailTemplateBuilder;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Base64;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final AccountTokenRepository accountTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final JavaMailSender mailSender;

    @Value("${app.otp.expiration-minutes}")
    private int otpExpirationMinutes;

    @Value("${app.otp.length}")
    private int otpLength;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public AuthServiceImpl(UserRepository userRepository,
                           AccountTokenRepository accountTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           AuthenticationManager authenticationManager,
                           JavaMailSender mailSender) {
        this.userRepository = userRepository;
        this.accountTokenRepository = accountTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.mailSender = mailSender;
    }

    // ==================== UC-04: Đăng ký tài khoản ====================
    @Override
    @Transactional
    public void register(RegisterReq request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("Email đã được sử dụng. Vui lòng dùng email khác.");
        }

        User user = new User(
                request.email().toLowerCase().trim(),
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                request.phoneNumber()
        );
        userRepository.save(user);

        String otp = generateOtp();
        saveOtpToken(user, otp);
        sendOtpEmail(user.getEmail(), user.getFullName(), otp);

        log.info("Đăng ký thành công cho email: {}. OTP đã gửi.", user.getEmail());
    }

    // ==================== UC-04: Xác thực OTP ====================
    @Override
    @Transactional
    public AuthResp verifyOtp(VerifyOtpReq request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản với email này."));

        if (user.isActive()) {
            throw new BadRequestException("Tài khoản đã được kích hoạt trước đó.");
        }

        AccountToken otpToken = accountTokenRepository
                .findFirstByUser_IdAndTokenTypeAndUsedAtIsNullOrderByCreatedAtDesc(
                        user.getId(), "email_verification")
                .orElseThrow(() -> new BadRequestException("Không tìm thấy mã OTP. Vui lòng yêu cầu gửi lại."));

        if (otpToken.isExpired()) {
            throw new BadRequestException("Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại.");
        }

        if (!passwordEncoder.matches(request.otp(), otpToken.getTokenHash())) {
            throw new BadRequestException("Mã OTP không chính xác. Vui lòng kiểm tra lại.");
        }

        otpToken.markUsed();
        accountTokenRepository.save(otpToken);

        user.setAccountStatus("active");
        user.setEmailVerifiedAt(Instant.now());
        userRepository.save(user);

        log.info("Xác thực OTP thành công. Tài khoản {} đã kích hoạt.", user.getEmail());
        return issueTokens(user);
    }

    // ==================== UC-03: Đăng nhập ====================
    @Override
    @Transactional
    public AuthResp login(LoginReq request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản."));

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("Đăng nhập thành công: {}", user.getEmail());
        return issueTokens(user);
    }

    // ==================== Gửi lại OTP ====================
    @Override
    @Transactional
    public void resendOtp(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản với email này."));

        if (user.isActive()) {
            throw new BadRequestException("Tài khoản đã được kích hoạt, không cần xác thực OTP.");
        }

        revokeAllOldOtps(user.getId());

        String otp = generateOtp();
        saveOtpToken(user, otp);
        sendOtpEmail(user.getEmail(), user.getFullName(), otp);

        log.info("Đã gửi lại OTP cho email: {}", user.getEmail());
    }

    // ==================== UC-05: Đăng xuất – Thu hồi Refresh Token ====================
    @Override
    @Transactional
    public void logout(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        AccountToken storedToken = accountTokenRepository
                .findByTokenHashAndTokenType(tokenHash, "refresh")
                .orElseThrow(() -> new BadRequestException("Refresh token không hợp lệ hoặc đã bị thu hồi."));

        if (storedToken.isUsed()) {
            log.warn("UC-05: Refresh token đã bị thu hồi trước đó (tokenId: {})", storedToken.getId());
            return;
        }

        storedToken.revoke();
        accountTokenRepository.save(storedToken);
        log.info("UC-05: Đăng xuất thành công – Refresh token đã bị thu hồi (tokenId: {})", storedToken.getId());
    }

    // ==================== UC-05: Cấp mới Access Token từ Refresh Token ====================
    @Override
    @Transactional
    public AuthResp refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Refresh token không hợp lệ hoặc đã hết hạn.");
        }

        String tokenHash = hashToken(refreshToken);
        AccountToken storedToken = accountTokenRepository
                .findByTokenHashAndTokenType(tokenHash, "refresh")
                .orElseThrow(() -> new BadRequestException("Refresh token không tồn tại trong hệ thống."));

        if (!storedToken.isValid()) {
            throw new BadRequestException("Phiên đăng nhập đã hết hạn hoặc bị thu hồi. Vui lòng đăng nhập lại.");
        }

        Integer userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản."));

        if (!user.isActive()) {
            throw new BadRequestException("Tài khoản đã bị vô hiệu hóa. Không thể cấp token mới.");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getSystemRole());

        log.info("UC-05: Đã cấp access token mới cho user: {}", user.getEmail());

        return AuthResp.of(newAccessToken, refreshToken,
                user.getId(), user.getEmail(), user.getFullName(), user.getSystemRole());
    }

    // ==================== UC-06: Quên mật khẩu – Gửi OTP ====================
    @Override
    @Transactional
    public void forgotPassword(String email) {
        var userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            log.warn("UC-06: Yêu cầu quên mật khẩu cho email không tồn tại: {}", email);
            return;
        }

        User user = userOpt.get();
        if (!user.isActive()) {
            log.warn("UC-06: Yêu cầu quên mật khẩu cho tài khoản chưa kích hoạt: {}", email);
            return;
        }

        revokeAllOldTokens(user.getId(), "password_reset");

        String otp = generateOtp();
        savePasswordResetOtp(user, otp);
        sendPasswordResetEmail(user.getEmail(), user.getFullName(), otp);

        log.info("UC-06: Đã gửi OTP đặt lại mật khẩu cho email: {}", user.getEmail());
    }

    // ==================== UC-06: Đặt lại mật khẩu ====================
    @Override
    @Transactional
    public void resetPassword(ResetPasswordReq request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản với email này."));

        if (!user.isActive()) {
            throw new BadRequestException("Tài khoản chưa được kích hoạt. Vui lòng xác thực email trước.");
        }

        AccountToken resetToken = accountTokenRepository
                .findFirstByUser_IdAndTokenTypeAndUsedAtIsNullOrderByCreatedAtDesc(
                        user.getId(), "password_reset")
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy mã OTP đặt lại mật khẩu. Vui lòng yêu cầu gửi lại."));

        if (resetToken.isExpired()) {
            throw new BadRequestException("Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại.");
        }

        if (!passwordEncoder.matches(request.otp(), resetToken.getTokenHash())) {
            throw new BadRequestException("Mã OTP không chính xác. Vui lòng kiểm tra lại.");
        }

        resetToken.markUsed();
        accountTokenRepository.save(resetToken);

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        revokeAllOldTokens(user.getId(), "refresh");

        log.info("UC-06: Đặt lại mật khẩu thành công cho email: {}", user.getEmail());
    }

    // ==================== Google OAuth2 Sign-In / Register ====================
    @Override
    @Transactional
    public AuthResp loginWithGoogle(GoogleAuthReq request) {
        if (request == null) {
            throw new BadRequestException("Dữ liệu xác thực Google không hợp lệ.");
        }

        String email = request.email();
        String fullName = request.fullName();
        String avatarUrl = request.avatarUrl();

        if (request.idToken() != null && !request.idToken().isBlank()) {
            String[] parts = request.idToken().split("\\.");
            if (parts.length >= 2) {
                try {
                    byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode claims = mapper.readTree(decoded);
                    if (claims.hasNonNull("email")) {
                        email = claims.get("email").asText();
                    }
                    if (claims.hasNonNull("name") && (fullName == null || fullName.isBlank())) {
                        fullName = claims.get("name").asText();
                    }
                    if (claims.hasNonNull("picture") && (avatarUrl == null || avatarUrl.isBlank())) {
                        avatarUrl = claims.get("picture").asText();
                    }
                } catch (Exception e) {
                    log.warn("Không thể giải mã Google ID token payload: {}", e.getMessage());
                }
            }
        }

        if (email == null || email.isBlank()) {
            throw new BadRequestException("Không xác định được email từ tài khoản Google.");
        }

        email = email.trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);

        if (user != null) {
            if (!user.isActive()) {
                user.setAccountStatus("active");
                user.setEmailVerifiedAt(Instant.now());
            }
            if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) && avatarUrl != null && !avatarUrl.isBlank()) {
                user.setAvatarUrl(avatarUrl);
            }
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
            log.info("Google Sign-In thành công cho tài khoản hiện có: {}", user.getEmail());
            return issueTokens(user);
        } else {
            String randomPassword = UUID.randomUUID().toString();
            String name = (fullName != null && !fullName.isBlank()) ? fullName.trim() : email.split("@")[0];
            User newUser = new User(
                    email,
                    passwordEncoder.encode(randomPassword),
                    name,
                    null,
                    "user",
                    "active"
            );
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                newUser.setAvatarUrl(avatarUrl);
            }
            newUser.setEmailVerifiedAt(Instant.now());
            newUser.setLastLoginAt(Instant.now());
            User saved = userRepository.save(newUser);
            log.info("Tạo mới tài khoản và đăng nhập thành công qua Google: {}", saved.getEmail());
            return issueTokens(saved);
        }
    }

    // ==================== Private helpers ====================

    private AuthResp issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getSystemRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        AccountToken refreshTokenEntity = AccountToken.createRefreshToken(
                user,
                hashToken(refreshToken),
                Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs())
        );
        accountTokenRepository.save(refreshTokenEntity);

        return AuthResp.of(accessToken, refreshToken,
                user.getId(), user.getEmail(), user.getFullName(), user.getSystemRole());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Thuật toán SHA-256 không khả dụng", e);
        }
    }

    private String generateOtp() {
        int bound = (int) Math.pow(10, otpLength);
        int otp = SECURE_RANDOM.nextInt(bound);
        return String.format("%0" + otpLength + "d", otp);
    }

    private void saveOtpToken(User user, String otp) {
        revokeAllOldOtps(user.getId());

        AccountToken otpToken = AccountToken.createOtp(
                user,
                passwordEncoder.encode(otp),
                Instant.now().plus(otpExpirationMinutes, ChronoUnit.MINUTES)
        );
        accountTokenRepository.save(otpToken);
    }

    private void savePasswordResetOtp(User user, String otp) {
        AccountToken otpToken = AccountToken.createPasswordResetOtp(
                user,
                passwordEncoder.encode(otp),
                Instant.now().plus(otpExpirationMinutes, ChronoUnit.MINUTES)
        );
        accountTokenRepository.save(otpToken);
    }

    private void revokeAllOldOtps(Integer userId) {
        List<AccountToken> oldTokens = accountTokenRepository
                .findAllByUser_IdAndTokenTypeAndUsedAtIsNull(userId, "email_verification");
        oldTokens.forEach(AccountToken::revoke);
        accountTokenRepository.saveAll(oldTokens);
    }

    private void revokeAllOldTokens(Integer userId, String tokenType) {
        List<AccountToken> oldTokens = accountTokenRepository
                .findAllByUser_IdAndTokenTypeAndUsedAtIsNull(userId, tokenType);
        oldTokens.forEach(AccountToken::revoke);
        accountTokenRepository.saveAll(oldTokens);
    }

    private void sendOtpEmail(String toEmail, String fullName, String otp) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            if (mimeMessage == null) {
                mimeMessage = new JavaMailSenderImpl().createMimeMessage();
            }
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom, "MyCropDiary");
            }
            helper.setTo(toEmail);
            helper.setSubject("[MyCropDiary] Mã xác thực tài khoản (OTP)");
            String htmlContent = EmailTemplateBuilder.buildRegistrationOtpEmail(fullName, otp, otpExpirationMinutes);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("OTP email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Không thể gửi email OTP tới {}: {}", toEmail, e.getMessage());
            log.info("OTP cho {}: {} (hiển thị trong log do không gửi được email)", toEmail, otp);
        }
    }

    private void sendPasswordResetEmail(String toEmail, String fullName, String otp) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            if (mimeMessage == null) {
                mimeMessage = new JavaMailSenderImpl().createMimeMessage();
            }
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom, "MyCropDiary");
            }
            helper.setTo(toEmail);
            helper.setSubject("[MyCropDiary] Mã OTP đặt lại mật khẩu");
            String htmlContent = EmailTemplateBuilder.buildPasswordResetOtpEmail(fullName, otp, otpExpirationMinutes);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Password reset OTP email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Không thể gửi email đặt lại mật khẩu tới {}: {}", toEmail, e.getMessage());
            log.info("Password reset OTP cho {}: {} (hiển thị trong log do không gửi được email)", toEmail, otp);
        }
    }
}
