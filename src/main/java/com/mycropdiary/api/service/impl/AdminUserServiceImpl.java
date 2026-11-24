package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.admin.CreateAdminUserReqDTO;
import com.mycropdiary.api.dto.admin.UpdateUserStatusReqDTO;
import com.mycropdiary.api.dto.admin.UserRespDTO;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.entity.AccountToken;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffZoneAssignment;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceConflictException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.AccountTokenRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.mapper.UserMapper;
import com.mycropdiary.api.service.admin.AdminUserService;
import com.mycropdiary.api.util.EmailTemplateBuilder;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserServiceImpl.class);

    private final UserRepository userRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final StaffZoneAssignmentRepository staffZoneAssignmentRepository;
    private final AccountTokenRepository accountTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${app.otp.expiration-minutes:10}")
    private long otpExpirationMinutes;

    @org.springframework.beans.factory.annotation.Autowired
    public AdminUserServiceImpl(
            UserRepository userRepository,
            FarmMemberRepository farmMemberRepository,
            StaffZoneAssignmentRepository staffZoneAssignmentRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) AccountTokenRepository accountTokenRepository,
            @org.springframework.beans.factory.annotation.Autowired(required = false) PasswordEncoder passwordEncoder,
            @org.springframework.beans.factory.annotation.Autowired(required = false) JavaMailSender mailSender
    ) {
        this.userRepository = userRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.staffZoneAssignmentRepository = staffZoneAssignmentRepository;
        this.accountTokenRepository = accountTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    public AdminUserServiceImpl(UserRepository userRepository) {
        this(userRepository, null, null, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResp<UserRespDTO> getUsers(String status, String search, Pageable pageable) {
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toLowerCase() : null;
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<User> page = userRepository.searchUsers(cleanStatus, cleanSearch, pageable);
        Map<Integer, List<FarmMember>> membersByUser = farmMemberRepository == null ? Collections.emptyMap() : farmMemberRepository.findAll().stream()
                .filter(m -> m.getUser() != null && m.getUser().getId() != null && m.isActive())
                .collect(Collectors.groupingBy(m -> m.getUser().getId()));
        Map<Integer, List<StaffZoneAssignment>> zonesByUser = staffZoneAssignmentRepository == null ? Collections.emptyMap() : staffZoneAssignmentRepository.findAll().stream()
                .filter(a -> a.getFarmMember() != null && a.getFarmMember().getUser() != null && a.getFarmMember().getUser().getId() != null && a.isActive())
                .collect(Collectors.groupingBy(a -> a.getFarmMember().getUser().getId()));

        return PageResp.from(page.map(u -> toEnrichedUserResp(
                u,
                membersByUser.getOrDefault(u.getId(), Collections.emptyList()),
                zonesByUser.getOrDefault(u.getId(), Collections.emptyList())
        )));
    }

    @Override
    @Transactional(readOnly = true)
    public UserRespDTO getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với mã ID: " + userId));
        return enrichSingleUser(user);
    }

    @Override
    public UserRespDTO updateUserStatus(Integer adminUserId, Integer targetUserId, UpdateUserStatusReqDTO request) {
        String newStatus = request.status().trim().toLowerCase();

        if (adminUserId != null && adminUserId.equals(targetUserId) && "locked".equalsIgnoreCase(newStatus)) {
            throw new BadRequestException("Quản trị viên không thể tự khóa tài khoản của chính mình.");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với mã ID: " + targetUserId));

        user.setAccountStatus(newStatus);
        User savedUser = userRepository.save(user);

        return enrichSingleUser(savedUser);
    }

    @Override
    public UserRespDTO updateUser(Integer adminUserId, Integer targetUserId, com.mycropdiary.api.dto.admin.UpdateUserReqDTO request) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với mã ID: " + targetUserId));

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        if (request.phone() != null) {
            String phone = request.phone().trim();
            user.setPhone(phone.isEmpty() ? null : phone);
        }
        if (request.systemRole() != null && !request.systemRole().isBlank()) {
            String role = request.systemRole().trim().toLowerCase();
            if (adminUserId != null && adminUserId.equals(targetUserId) && !"admin".equalsIgnoreCase(role)) {
                throw new BadRequestException("Quản trị viên không thể tự hạ quyền của chính mình.");
            }
            user.setSystemRole(role);
        }
        if (request.accountStatus() != null && !request.accountStatus().isBlank()) {
            String status = request.accountStatus().trim().toLowerCase();
            if (adminUserId != null && adminUserId.equals(targetUserId) && "locked".equalsIgnoreCase(status)) {
                throw new BadRequestException("Quản trị viên không thể tự khóa tài khoản của chính mình.");
            }
            user.setAccountStatus(status);
        }

        User saved = userRepository.save(user);
        return enrichSingleUser(saved);
    }

    @Override
    public UserRespDTO updateUserProfile(Integer adminUserId, Integer targetUserId, Map<String, Object> request) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với mã ID: " + targetUserId));

        if (request.containsKey("fullName") && request.get("fullName") != null) {
            String fullName = String.valueOf(request.get("fullName")).trim();
            if (!fullName.isEmpty()) {
                user.setFullName(fullName);
            }
        }
        if (request.containsKey("phone")) {
            String phone = request.get("phone") != null ? String.valueOf(request.get("phone")).trim() : null;
            user.setPhone(phone);
        }
        if (request.containsKey("systemRole") && request.get("systemRole") != null) {
            String role = String.valueOf(request.get("systemRole")).trim().toLowerCase();
            if (adminUserId != null && adminUserId.equals(targetUserId) && !"admin".equalsIgnoreCase(role)) {
                throw new BadRequestException("Quản trị viên không thể tự hạ quyền của chính mình.");
            }
            user.setSystemRole(role);
        }
        if (request.containsKey("accountStatus") && request.get("accountStatus") != null) {
            String status = String.valueOf(request.get("accountStatus")).trim().toLowerCase();
            if (adminUserId != null && adminUserId.equals(targetUserId) && "locked".equalsIgnoreCase(status)) {
                throw new BadRequestException("Quản trị viên không thể tự khóa tài khoản của chính mình.");
            }
            user.setAccountStatus(status);
        }

        User saved = userRepository.save(user);
        return enrichSingleUser(saved);
    }

    @Override
    public UserRespDTO createUser(Integer adminUserId, CreateAdminUserReqDTO request) {
        String cleanEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new ResourceConflictException("Email '" + cleanEmail + "' đã được sử dụng trong hệ thống.");
        }

        String rawPassword = (request.password() != null && !request.password().isBlank())
                ? request.password().trim()
                : generateRandomPassword();

        String encodedPassword = passwordEncoder != null
                ? passwordEncoder.encode(rawPassword)
                : rawPassword;

        String role = (request.systemRole() != null && !request.systemRole().isBlank())
                ? request.systemRole().trim().toLowerCase()
                : "user";

        User user = new User();
        user.setEmail(cleanEmail);
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone() != null ? request.phone().trim() : null);
        user.setPasswordHash(encodedPassword);
        user.setSystemRole(role);
        user.setAccountStatus("pending");

        User savedUser = userRepository.save(user);

        // Sinh OTP kích hoạt và gửi email
        try {
            String otp = generateOtp();
            if (accountTokenRepository != null && passwordEncoder != null) {
                String hashedOtp = passwordEncoder.encode(otp);
                AccountToken otpToken = AccountToken.createOtp(savedUser, hashedOtp, Instant.now().plus(otpExpirationMinutes, ChronoUnit.MINUTES));
                accountTokenRepository.save(otpToken);
            }
            sendActivationEmail(savedUser.getEmail(), savedUser.getFullName(), otp, rawPassword);
        } catch (Exception e) {
            log.error("Không thể gửi email kích hoạt cho user mới {}: {}", cleanEmail, e.getMessage());
        }

        return enrichSingleUser(savedUser);
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private void sendActivationEmail(String toEmail, String fullName, String otp, String temporaryPassword) {
        if (mailSender == null) {
            log.info("[DEV] Mock gửi email kích hoạt cho {}: OTP={}, Password={}", toEmail, otp, temporaryPassword);
            return;
        }
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
            helper.setSubject("[MyCropDiary] Kích hoạt tài khoản người dùng mới");

            String html = EmailTemplateBuilder.buildRegistrationOtpEmail(fullName, otp, otpExpirationMinutes);
            helper.setText(html, true);

            mailSender.send(mimeMessage);
            log.info("Activation email with OTP sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email kích hoạt tới {}: {}", toEmail, e.getMessage());
        }
    }

    private UserRespDTO enrichSingleUser(User user) {
        List<FarmMember> memberships = farmMemberRepository == null ? Collections.emptyList() : farmMemberRepository.findAll().stream()
                .filter(m -> m.getUser() != null && user.getId().equals(m.getUser().getId()) && m.isActive())
                .toList();
        List<StaffZoneAssignment> zoneAssignments = staffZoneAssignmentRepository == null ? Collections.emptyList() : staffZoneAssignmentRepository.findAll().stream()
                .filter(a -> a.getFarmMember() != null && a.getFarmMember().getUser() != null && user.getId().equals(a.getFarmMember().getUser().getId()) && a.isActive())
                .toList();
        return toEnrichedUserResp(user, memberships, zoneAssignments);
    }

    private final UserMapper userMapper = new UserMapper();

    private UserRespDTO toEnrichedUserResp(User user, List<FarmMember> memberships, List<StaffZoneAssignment> zoneAssignments) {
        return userMapper.toEnrichedUserResp(user, memberships, zoneAssignments);
    }
}
