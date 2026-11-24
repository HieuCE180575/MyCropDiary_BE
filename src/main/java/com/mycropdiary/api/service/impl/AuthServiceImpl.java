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

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_PASSWORD_BYTES = 72;
    private static final int MAX_FULL_NAME_LENGTH = 150;
    private static final String GOOGLE_CERTS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Map<String, PublicKey> googleJwkCache = new ConcurrentHashMap<>();

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
    public void register(RegisterAccReqDTO request) {
        String trimmedEmail = request.email().trim();
        String normalizedEmail = trimmedEmail.toLowerCase();
        String trimmedFullName = request.fullName() != null ? request.fullName().trim() : "";
        if (trimmedFullName.isEmpty() || trimmedFullName.length() > MAX_FULL_NAME_LENGTH) {
            throw new BadRequestException("Họ tên phải từ 1 đến " + MAX_FULL_NAME_LENGTH + " ký tự.");
        }
        String normalizedPassword = normalizeAndValidatePassword(request.password());

        if (userRepository.existsByEmailIgnoreCase(trimmedEmail)) {
            throw new BadRequestException("Email đã được sử dụng. Vui lòng dùng email khác.");
        }

        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(normalizedPassword),
                trimmedFullName,
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
    @Transactional(noRollbackFor = BadRequestException.class)
    public AuthRespDTO verifyOtp(VerifyOtpReqDTO request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản với email này."));

        if (user.isActive()) {
            throw new BadRequestException("Tài khoản đã được kích hoạt trước đó.");
        }

        AccountToken otpToken = accountTokenRepository
                .findFirstByUser_IdAndTokenTypeAndUsedAtIsNullOrderByCreatedAtDesc(
                        user.getId(), "email_verification")
                .orElseThrow(() -> new BadRequestException("Không tìm thấy mã OTP. Vui lòng yêu cầu gửi lại."));

        verifyOtpTokenOrThrow(otpToken, request.otp());

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
    public AuthRespDTO login(LoginReqDTO request) {
        String normalizedEmail = request.email() != null ? request.email().trim() : "";
        String normalizedPassword = normalizeAndValidatePassword(request.password());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, normalizedPassword)
        );

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
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
    public AuthRespDTO refreshToken(String refreshToken) {
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

        return AuthRespDTO.of(newAccessToken, refreshToken,
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
    @Transactional(noRollbackFor = BadRequestException.class)
    public void resetPassword(ResetPasswordReqDTO request) {
        String normalizedNewPassword = normalizeAndValidatePassword(request.newPassword());

        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản với email này."));

        if (!user.isActive()) {
            throw new BadRequestException("Tài khoản chưa được kích hoạt. Vui lòng xác thực email trước.");
        }

        AccountToken resetToken = accountTokenRepository
                .findFirstByUser_IdAndTokenTypeAndUsedAtIsNullOrderByCreatedAtDesc(
                        user.getId(), "password_reset")
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy mã OTP đặt lại mật khẩu. Vui lòng yêu cầu gửi lại."));

        verifyOtpTokenOrThrow(resetToken, request.otp());

        if (user.getPasswordHash() != null && passwordEncoder.matches(normalizedNewPassword, user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu hiện tại.");
        }

        resetToken.markUsed();
        accountTokenRepository.save(resetToken);

        user.setPasswordHash(passwordEncoder.encode(normalizedNewPassword));
        userRepository.save(user);

        revokeAllOldTokens(user.getId(), "refresh");

        log.info("UC-06: Đặt lại mật khẩu thành công cho email: {}", user.getEmail());
    }

    // ==================== Google OAuth2 Sign-In / Register ====================
    @Override
    @Transactional
    public AuthRespDTO loginWithGoogle(GoogleAuthReqDTO request) {
        if (request == null) {
            throw new BadRequestException("Dữ liệu xác thực Google không hợp lệ.");
        }

        String email = request.email();
        String fullName = request.fullName();
        String avatarUrl = request.avatarUrl();

        if (request.idToken() != null && !request.idToken().isBlank()) {
            JsonNode verifiedClaims = verifyGoogleIdToken(request.idToken().trim());
            if (verifiedClaims.hasNonNull("email")) {
                email = verifiedClaims.get("email").asText();
            }
            if (verifiedClaims.hasNonNull("name") && (fullName == null || fullName.isBlank())) {
                fullName = verifiedClaims.get("name").asText();
            }
            if (verifiedClaims.hasNonNull("picture") && (avatarUrl == null || avatarUrl.isBlank())) {
                avatarUrl = verifiedClaims.get("picture").asText();
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

    private String normalizeAndValidatePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new BadRequestException("Mật khẩu không được để trống.");
        }
        String trimmed = rawPassword.trim();
        if (trimmed.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new BadRequestException("Mật khẩu không được vượt quá 72 bytes.");
        }
        return trimmed;
    }

    private void verifyOtpTokenOrThrow(AccountToken otpToken, String rawOtp) {
        if (otpToken.isExpired()) {
            throw new BadRequestException("Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại.");
        }
        if (otpToken.isUsed() || otpToken.isAttemptLimitReached()) {
            throw new BadRequestException("Mã OTP đã bị khóa do nhập sai quá " + AccountToken.MAX_FAILED_ATTEMPTS + " lần. Vui lòng yêu cầu gửi lại mã mới.");
        }
        if (!passwordEncoder.matches(rawOtp, otpToken.getTokenHash())) {
            int attempts = otpToken.incrementFailedAttempts();
            accountTokenRepository.save(otpToken);
            if (attempts >= AccountToken.MAX_FAILED_ATTEMPTS) {
                throw new BadRequestException("Mã OTP đã bị khóa do nhập sai quá " + AccountToken.MAX_FAILED_ATTEMPTS + " lần. Vui lòng yêu cầu gửi lại mã mới.");
            }
            throw new BadRequestException("Mã OTP không chính xác. Vui lòng kiểm tra lại.");
        }
    }

    protected JsonNode verifyGoogleIdToken(String idToken) {
        String[] parts = idToken.split("\\.");
        if (parts.length == 3) {
            try {
                JsonNode header = OBJECT_MAPPER.readTree(Base64.getUrlDecoder().decode(parts[0]));
                String alg = header.hasNonNull("alg") ? header.get("alg").asText() : "";
                String kid = header.hasNonNull("kid") ? header.get("kid").asText() : null;
                if ("RS256".equalsIgnoreCase(alg) && kid != null && !kid.isBlank()) {
                    PublicKey publicKey = resolveGooglePublicKey(kid);
                    if (publicKey != null) {
                        byte[] signedBytes = (parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII);
                        byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[2]);
                        Signature sig = Signature.getInstance("SHA256withRSA");
                        sig.initVerify(publicKey);
                        sig.update(signedBytes);
                        if (!sig.verify(signatureBytes)) {
                            throw new BadRequestException("Chữ ký Google ID Token không hợp lệ.");
                        }
                        JsonNode claims = OBJECT_MAPPER.readTree(Base64.getUrlDecoder().decode(parts[1]));
                        if (claims.hasNonNull("exp")) {
                            long exp = claims.get("exp").asLong();
                            if (Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
                                throw new BadRequestException("Google ID Token đã hết hạn.");
                            }
                        }
                        if (claims.hasNonNull("iss")) {
                            String iss = claims.get("iss").asText();
                            if (!"https://accounts.google.com".equals(iss) && !"accounts.google.com".equals(iss)) {
                                throw new BadRequestException("Google ID Token issuer không hợp lệ.");
                            }
                        }
                        return claims;
                    }
                }
            } catch (BadRequestException e) {
                throw e;
            } catch (Exception e) {
                log.warn("Xác thực chữ ký RSA Google ID Token thất bại: {}", e.getMessage());
            }
        }

        // Fallback to Google's authoritative tokeninfo endpoint (never trust unverified Base64 claims)
        try {
            java.net.http.HttpRequest verifyReq = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> verifyResp = java.net.http.HttpClient.newHttpClient()
                    .send(verifyReq, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (verifyResp.statusCode() == 200) {
                return OBJECT_MAPPER.readTree(verifyResp.body());
            }
        } catch (Exception ex) {
            log.warn("Google tokeninfo verify thất bại: {}", ex.getMessage());
        }
        throw new BadRequestException("Google ID Token không hợp lệ hoặc không thể xác thực chữ ký.");
    }

    protected PublicKey resolveGooglePublicKey(String kid) {
        PublicKey cached = googleJwkCache.get(kid);
        if (cached != null) {
            return cached;
        }
        try {
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(GOOGLE_CERTS_URL))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> resp = java.net.http.HttpClient.newHttpClient()
                    .send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode keys = OBJECT_MAPPER.readTree(resp.body()).path("keys");
                if (keys.isArray()) {
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    for (JsonNode k : keys) {
                        String keyId = k.path("kid").asText(null);
                        String nStr = k.path("n").asText(null);
                        String eStr = k.path("e").asText(null);
                        if (keyId != null && nStr != null && eStr != null) {
                            BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(nStr));
                            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(eStr));
                            PublicKey pk = kf.generatePublic(new RSAPublicKeySpec(modulus, exponent));
                            googleJwkCache.put(keyId, pk);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Không thể tải Google JWK certs: {}", ex.getMessage());
        }
        return googleJwkCache.get(kid);
    }

    private AuthRespDTO issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getSystemRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        AccountToken refreshTokenEntity = AccountToken.createRefreshToken(
                user,
                hashToken(refreshToken),
                Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs())
        );
        accountTokenRepository.save(refreshTokenEntity);

        return AuthRespDTO.of(accessToken, refreshToken,
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
