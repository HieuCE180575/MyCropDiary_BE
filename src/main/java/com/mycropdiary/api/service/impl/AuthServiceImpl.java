package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.auth.*;
import com.mycropdiary.api.entity.AccountToken;
import com.mycropdiary.api.entity.AppUser;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.repository.AccountTokenRepository;
import com.mycropdiary.api.repository.AppUserRepository;
import com.mycropdiary.api.security.JwtTokenProvider;
import com.mycropdiary.api.service.AuthService;
import com.mycropdiary.api.util.EmailTemplateBuilder;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
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

// [AI_CHANGE] Root cause: Cần triển khai nghiệp vụ đăng ký (tạo User PENDING + OTP), xác thực OTP, đăng nhập JWT
// [AI_CHANGE] Mechanism: register() -> hash password, save PENDING user, generate OTP, send email
//             verifyOtp() -> validate OTP, activate user, issue JWT
//             login() -> authenticate via AuthenticationManager, issue JWT
@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AppUserRepository appUserRepository;
    private final AccountTokenRepository accountTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final JavaMailSender mailSender;

    @Value("${app.otp.expiration-minutes}")
    private int otpExpirationMinutes;

    @Value("${app.otp.length}")
    private int otpLength;

    // [AI_CHANGE] Root cause: Gmail SMTP yêu cầu header From để tránh lỗi hostname local hoặc bị từ chối
    // [AI_CHANGE] Mechanism: Inject cấu hình spring.mail.username làm địa chỉ From mặc định
    @Value("${spring.mail.username:}")
    private String mailFrom;

    public AuthServiceImpl(AppUserRepository appUserRepository,
                           AccountTokenRepository accountTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider,
                           AuthenticationManager authenticationManager,
                           JavaMailSender mailSender) {
        this.appUserRepository = appUserRepository;
        this.accountTokenRepository = accountTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.mailSender = mailSender;
    }

    // ==================== UC-04: Đăng ký tài khoản ====================
    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // [AI_CHANGE] Kiểm tra email đã tồn tại
        if (appUserRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BadRequestException("Email đã được sử dụng. Vui lòng dùng email khác.");
        }

        // [AI_CHANGE] Tạo user mới với trạng thái PENDING, hash password bằng BCrypt
        AppUser user = new AppUser(
                request.email().toLowerCase().trim(),
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                request.phoneNumber()
        );
        appUserRepository.save(user);

        // [AI_CHANGE] Tạo OTP 6 số và gửi email xác thực
        String otp = generateOtp();
        saveOtpToken(user, otp);
        sendOtpEmail(user.getEmail(), user.getFullName(), otp);

        log.info("Đăng ký thành công cho email: {}. OTP đã gửi.", user.getEmail());
    }

    // ==================== UC-04: Xác thực OTP ====================
    @Override
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        // [AI_CHANGE] Tìm user theo email
        AppUser user = appUserRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản với email này."));

        if (user.isActive()) {
            throw new BadRequestException("Tài khoản đã được kích hoạt trước đó.");
        }

        // [AI_CHANGE] Tìm OTP hợp lệ mới nhất của user
        AccountToken otpToken = accountTokenRepository
                .findFirstByUser_IdAndTokenTypeAndUsedAtIsNullAndRevokedAtIsNullOrderByCreatedAtDesc(
                        user.getId(), "EMAIL_OTP")
                .orElseThrow(() -> new BadRequestException("Không tìm thấy mã OTP. Vui lòng yêu cầu gửi lại."));

        // [AI_CHANGE] Kiểm tra OTP hết hạn
        if (otpToken.isExpired()) {
            throw new BadRequestException("Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại.");
        }

        // [AI_CHANGE] So khớp OTP (đã hash bằng BCrypt)
        if (!passwordEncoder.matches(request.otp(), otpToken.getTokenHash())) {
            throw new BadRequestException("Mã OTP không chính xác. Vui lòng kiểm tra lại.");
        }

        // [AI_CHANGE] OTP hợp lệ -> kích hoạt tài khoản
        otpToken.markUsed();
        accountTokenRepository.save(otpToken);

        user.setAccountStatus("ACTIVE");
        user.setEmailVerifiedAt(Instant.now());
        appUserRepository.save(user);

        log.info("Xác thực OTP thành công. Tài khoản {} đã kích hoạt.", user.getEmail());

        // [AI_CHANGE] Cấp JWT tokens sau khi xác thực thành công
        return issueTokens(user);
    }

    // ==================== UC-03: Đăng nhập ====================
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // [AI_CHANGE] Xác thực thông qua AuthenticationManager
        // Nếu sai mật khẩu -> ném BadCredentialsException
        // Nếu tài khoản PENDING (disabled) -> ném DisabledException
        // Nếu tài khoản LOCKED -> ném LockedException
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        // [AI_CHANGE] Nếu xác thực thành công, lấy user và cấp JWT
        AppUser user = appUserRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản."));

        log.info("Đăng nhập thành công: {}", user.getEmail());
        return issueTokens(user);
    }

    // ==================== Gửi lại OTP ====================
    @Override
    @Transactional
    public void resendOtp(String email) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản với email này."));

        if (user.isActive()) {
            throw new BadRequestException("Tài khoản đã được kích hoạt, không cần xác thực OTP.");
        }

        // [AI_CHANGE] Thu hồi tất cả OTP cũ chưa dùng
        revokeAllOldOtps(user.getId());

        // [AI_CHANGE] Tạo OTP mới và gửi lại
        String otp = generateOtp();
        saveOtpToken(user, otp);
        sendOtpEmail(user.getEmail(), user.getFullName(), otp);

        log.info("Đã gửi lại OTP cho email: {}", user.getEmail());
    }

    // ==================== UC-05: Đăng xuất – Thu hồi Refresh Token ====================
    // [AI_CHANGE] Root cause: UC-05 cần cơ chế đăng xuất bằng cách vô hiệu hóa Refresh Token
    // [AI_CHANGE] Mechanism: Hash SHA-256 refresh token nhận từ client, tìm trong DB, revoke nó
    @Override
    @Transactional
    public void logout(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        AccountToken storedToken = accountTokenRepository
                .findByTokenHashAndTokenType(tokenHash, "REFRESH_TOKEN")
                .orElseThrow(() -> new BadRequestException("Refresh token không hợp lệ hoặc đã bị thu hồi."));

        if (storedToken.isRevoked()) {
            log.warn("UC-05: Refresh token đã bị thu hồi trước đó (tokenId: {})", storedToken.getId());
            return; // Idempotent – không ném lỗi nếu đã revoke
        }

        storedToken.revoke();
        accountTokenRepository.save(storedToken);
        log.info("UC-05: Đăng xuất thành công – Refresh token đã bị thu hồi (tokenId: {})", storedToken.getId());
    }

    // ==================== UC-05: Cấp mới Access Token từ Refresh Token ====================
    // [AI_CHANGE] Root cause: UC-05 cần cơ chế làm mới access token khi hết hạn mà không cần đăng nhập lại
    // [AI_CHANGE] Mechanism: Validate JWT refresh token, tìm hash trong DB (chưa revoked/used/expired),
    //             nếu hợp lệ cấp access token mới, giữ nguyên refresh token
    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        // Bước 1: Kiểm tra JWT hợp lệ (chữ ký, hạn sử dụng)
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Refresh token không hợp lệ hoặc đã hết hạn.");
        }

        // Bước 2: Kiểm tra token tồn tại trong DB và chưa bị thu hồi
        String tokenHash = hashToken(refreshToken);
        AccountToken storedToken = accountTokenRepository
                .findByTokenHashAndTokenType(tokenHash, "REFRESH_TOKEN")
                .orElseThrow(() -> new BadRequestException("Refresh token không tồn tại trong hệ thống."));

        if (!storedToken.isValid()) {
            throw new BadRequestException("Phiên đăng nhập đã hết hạn hoặc bị thu hồi. Vui lòng đăng nhập lại.");
        }

        // Bước 3: Lấy user từ token và cấp access token mới
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản."));

        if (!user.isActive()) {
            throw new BadRequestException("Tài khoản đã bị vô hiệu hóa. Không thể cấp token mới.");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getSystemRole());

        log.info("UC-05: Đã cấp access token mới cho user: {}", user.getEmail());

        // Giữ nguyên refresh token hiện tại
        return AuthResponse.of(newAccessToken, refreshToken,
                user.getId(), user.getEmail(), user.getFullName(), user.getSystemRole());
    }

    // ==================== UC-06: Quên mật khẩu – Gửi OTP ====================
    // [AI_CHANGE] Root cause: UC-06 người dùng quên mật khẩu cần cơ chế gửi OTP qua email để đặt lại
    // [AI_CHANGE] Mechanism: Tạo OTP type PASSWORD_RESET, gửi email; không tiết lộ email có tồn tại hay không
    @Override
    @Transactional
    public void forgotPassword(String email) {
        // [AI_CHANGE] Tìm user theo email; nếu không tìm thấy vẫn trả về thành công để tránh lộ thông tin
        var userOpt = appUserRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            log.warn("UC-06: Yêu cầu quên mật khẩu cho email không tồn tại: {}", email);
            return; // Không tiết lộ email có tồn tại hay không (bảo mật)
        }

        AppUser user = userOpt.get();

        if (!user.isActive()) {
            log.warn("UC-06: Yêu cầu quên mật khẩu cho tài khoản chưa kích hoạt: {}", email);
            return; // Tương tự, không tiết lộ trạng thái tài khoản
        }

        // [AI_CHANGE] Thu hồi tất cả OTP PASSWORD_RESET cũ chưa dùng
        revokeAllOldTokens(user.getId(), "PASSWORD_RESET");

        // [AI_CHANGE] Tạo OTP mới type PASSWORD_RESET và gửi email
        String otp = generateOtp();
        savePasswordResetOtp(user, otp);
        sendPasswordResetEmail(user.getEmail(), user.getFullName(), otp);

        log.info("UC-06: Đã gửi OTP đặt lại mật khẩu cho email: {}", user.getEmail());
    }

    // ==================== UC-06: Đặt lại mật khẩu ====================
    // [AI_CHANGE] Root cause: UC-06 sau khi nhận OTP, người dùng cần API đặt lại mật khẩu mới
    // [AI_CHANGE] Mechanism: Xác minh OTP PASSWORD_RESET -> hash mật khẩu mới -> lưu DB -> vô hiệu OTP
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy tài khoản với email này."));

        if (!user.isActive()) {
            throw new BadRequestException("Tài khoản chưa được kích hoạt. Vui lòng xác thực email trước.");
        }

        // [AI_CHANGE] Tìm OTP PASSWORD_RESET hợp lệ mới nhất
        AccountToken resetToken = accountTokenRepository
                .findFirstByUser_IdAndTokenTypeAndUsedAtIsNullAndRevokedAtIsNullOrderByCreatedAtDesc(
                        user.getId(), "PASSWORD_RESET")
                .orElseThrow(() -> new BadRequestException(
                        "Không tìm thấy mã OTP đặt lại mật khẩu. Vui lòng yêu cầu gửi lại."));

        // [AI_CHANGE] Kiểm tra OTP hết hạn
        if (resetToken.isExpired()) {
            throw new BadRequestException("Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại.");
        }

        // [AI_CHANGE] So khớp OTP (đã hash bằng BCrypt)
        if (!passwordEncoder.matches(request.otp(), resetToken.getTokenHash())) {
            throw new BadRequestException("Mã OTP không chính xác. Vui lòng kiểm tra lại.");
        }

        // [AI_CHANGE] OTP hợp lệ -> Đặt lại mật khẩu mới
        resetToken.markUsed();
        accountTokenRepository.save(resetToken);

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);

        // [AI_CHANGE] Thu hồi tất cả refresh token cũ để buộc đăng nhập lại với mật khẩu mới
        revokeAllOldTokens(user.getId(), "REFRESH_TOKEN");

        log.info("UC-06: Đặt lại mật khẩu thành công cho email: {}", user.getEmail());
    }

    // ==================== Private helpers ====================

    private AuthResponse issueTokens(AppUser user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getSystemRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // [AI_CHANGE] Root cause: BCrypt chỉ nhận tối đa 72 bytes. Refresh Token (JWT) có độ dài > 150 bytes,
        //             dẫn tới lỗi IllegalArgumentException: password cannot be more than 72 bytes.
        // [AI_CHANGE] Mechanism: Dùng SHA-256 băm Refresh Token thành chuỗi hex 64 ký tự an toàn và cho phép query trực tiếp
        AccountToken refreshTokenEntity = AccountToken.createRefreshToken(
                user,
                hashToken(refreshToken),
                Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs())
        );
        accountTokenRepository.save(refreshTokenEntity);

        return AuthResponse.of(accessToken, refreshToken,
                user.getId(), user.getEmail(), user.getFullName(), user.getSystemRole());
    }

    // [AI_CHANGE] Cryptographic SHA-256 hash cho high-entropy tokens (Refresh Token)
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

    private void saveOtpToken(AppUser user, String otp) {
        // [AI_CHANGE] Thu hồi OTP cũ trước khi tạo mới
        revokeAllOldOtps(user.getId());

        AccountToken otpToken = AccountToken.createOtp(
                user,
                passwordEncoder.encode(otp),
                Instant.now().plus(otpExpirationMinutes, ChronoUnit.MINUTES)
        );
        accountTokenRepository.save(otpToken);
    }

    // [AI_CHANGE] UC-06: Lưu OTP đặt lại mật khẩu (type PASSWORD_RESET)
    private void savePasswordResetOtp(AppUser user, String otp) {
        AccountToken otpToken = AccountToken.createPasswordResetOtp(
                user,
                passwordEncoder.encode(otp),
                Instant.now().plus(otpExpirationMinutes, ChronoUnit.MINUTES)
        );
        accountTokenRepository.save(otpToken);
    }

    private void revokeAllOldOtps(Long userId) {
        List<AccountToken> oldTokens = accountTokenRepository
                .findAllByUser_IdAndTokenTypeAndUsedAtIsNullAndRevokedAtIsNull(userId, "EMAIL_OTP");
        oldTokens.forEach(AccountToken::revoke);
        accountTokenRepository.saveAll(oldTokens);
    }

    // [AI_CHANGE] UC-06/UC-05: Thu hồi tất cả token cũ theo type (PASSWORD_RESET hoặc REFRESH_TOKEN)
    private void revokeAllOldTokens(Long userId, String tokenType) {
        List<AccountToken> oldTokens = accountTokenRepository
                .findAllByUser_IdAndTokenTypeAndUsedAtIsNullAndRevokedAtIsNull(userId, tokenType);
        oldTokens.forEach(AccountToken::revoke);
        accountTokenRepository.saveAll(oldTokens);
    }

    private void sendOtpEmail(String toEmail, String fullName, String otp) {
        try {
            // [AI_CHANGE] Root cause: Người dùng yêu cầu email giao diện chuyên nghiệp, bắt mắt
            // [AI_CHANGE] Mechanism: Dùng MimeMessageHelper gửi email HTML đa nền tảng với nhận diện MyCropDiary VietGAP
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
            // [AI_CHANGE] Log lỗi nhưng không ném exception để user vẫn được tạo
            // Trong production cần retry mechanism hoặc message queue
            log.error("Không thể gửi email OTP tới {}: {}", toEmail, e.getMessage());
            log.info("OTP cho {}: {} (hiển thị trong log do không gửi được email)", toEmail, otp);
        }
    }

    // [AI_CHANGE] UC-06: Email HTML riêng cho luồng quên mật khẩu, phong cách trực quan, bắt mắt
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
