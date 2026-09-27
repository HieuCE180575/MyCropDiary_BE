package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.auth.*;
import com.mycropdiary.api.entity.AccountToken;
import com.mycropdiary.api.entity.AppUser;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.repository.AccountTokenRepository;
import com.mycropdiary.api.repository.AppUserRepository;
import com.mycropdiary.api.security.JwtTokenProvider;
import com.mycropdiary.api.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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

    private void revokeAllOldOtps(Long userId) {
        List<AccountToken> oldTokens = accountTokenRepository
                .findAllByUser_IdAndTokenTypeAndUsedAtIsNullAndRevokedAtIsNull(userId, "EMAIL_OTP");
        oldTokens.forEach(AccountToken::revoke);
        accountTokenRepository.saveAll(oldTokens);
    }

    private void sendOtpEmail(String toEmail, String fullName, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (mailFrom != null && !mailFrom.isBlank()) {
                message.setFrom(mailFrom);
            }
            message.setTo(toEmail);
            message.setSubject("[MyCropDiary] Mã xác thực tài khoản (OTP)");
            message.setText(String.format(
                    """
                    Xin chào %s,

                    Mã xác thực (OTP) của bạn là: %s

                    Mã này có hiệu lực trong %d phút. Vui lòng không chia sẻ mã này với bất kỳ ai.

                    Nếu bạn không yêu cầu đăng ký tài khoản MyCropDiary, vui lòng bỏ qua email này.

                    Trân trọng,
                    Đội ngũ MyCropDiary
                    """,
                    fullName, otp, otpExpirationMinutes
            ));
            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);
        } catch (Exception e) {
            // [AI_CHANGE] Log lỗi nhưng không ném exception để user vẫn được tạo
            // Trong production cần retry mechanism hoặc message queue
            log.error("Không thể gửi email OTP tới {}: {}", toEmail, e.getMessage());
            log.info("OTP cho {}: {} (hiển thị trong log do không gửi được email)", toEmail, otp);
        }
    }
}
