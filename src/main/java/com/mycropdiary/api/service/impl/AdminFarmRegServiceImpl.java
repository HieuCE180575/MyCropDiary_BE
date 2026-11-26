package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.farm.AdminFarmRegRespDTO;
import com.mycropdiary.api.dto.farm.HandleFarmRegReqDTO;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.farmregistration.FarmRegistration;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.AdminFarmRegMapper;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.farmregistration.FarmRegistrationRepository;
import com.mycropdiary.api.service.NotificationService;
import com.mycropdiary.api.service.farmregistration.AdminFarmRegService;
import com.mycropdiary.api.util.EmailTemplateBuilder;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AdminFarmRegServiceImpl implements AdminFarmRegService {

    private static final Logger log = LoggerFactory.getLogger(AdminFarmRegServiceImpl.class);

    private final FarmRegistrationRepository farmRegistrationRepository;
    private final FarmRepository farmRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AdminFarmRegMapper adminFarmRegMapper;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Autowired
    public AdminFarmRegServiceImpl(
            FarmRegistrationRepository farmRegistrationRepository,
            UserRepository userRepository,
            FarmRepository farmRepository,
            FarmMemberRepository farmMemberRepository,
            NotificationService notificationService,
            AdminFarmRegMapper adminFarmRegMapper) {
        this.farmRegistrationRepository = farmRegistrationRepository;
        this.userRepository = userRepository;
        this.farmRepository = farmRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.notificationService = notificationService;
        this.adminFarmRegMapper = adminFarmRegMapper != null ? adminFarmRegMapper : new AdminFarmRegMapper();
    }

    public AdminFarmRegServiceImpl(
            FarmRegistrationRepository farmRegistrationRepository,
            FarmRepository farmRepository,
            FarmMemberRepository farmMemberRepository,
            UserRepository userRepository,
            NotificationService notificationService) {
        this(farmRegistrationRepository, userRepository, farmRepository, farmMemberRepository, notificationService, new AdminFarmRegMapper());
    }

    @Override
    public PageResp<AdminFarmRegRespDTO> getRegistrations(String status, Pageable pageable) {
        Page<FarmRegistration> page;
        if (status != null && !status.isBlank()) {
            page = farmRegistrationRepository.findByStatusString(status.trim().toLowerCase(), pageable);
        } else {
            page = farmRegistrationRepository.findAll(pageable);
        }
        return PageResp.map(page, adminFarmRegMapper::toAdminResponse);
    }

    @Override
    public AdminFarmRegRespDTO getRegistrationById(Integer registrationId) {
        FarmRegistration registration = farmRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn đăng ký trang trại với ID: " + registrationId));
        return adminFarmRegMapper.toAdminResponse(registration);
    }

    @Override
    @Transactional
    public AdminFarmRegRespDTO updateRegistration(Integer registrationId, Map<String, Object> updates) {
        FarmRegistration registration = farmRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn đăng ký trang trại với ID: " + registrationId));

        if (updates != null) {
            Object farmName = updates.get("farmName") != null ? updates.get("farmName") : updates.get("name");
            if (farmName != null && !String.valueOf(farmName).isBlank()) {
                registration.setFarmName(String.valueOf(farmName).trim());
            }
            Object address = updates.get("addressLine") != null ? updates.get("addressLine") : updates.get("address");
            if (address != null && !String.valueOf(address).isBlank()) {
                registration.setAddress(String.valueOf(address).trim());
            }
            if (updates.containsKey("province")) {
                Object v = updates.get("province");
                registration.setProvince(v != null && !String.valueOf(v).isBlank() ? String.valueOf(v).trim() : null);
            }
            if (updates.containsKey("district")) {
                Object v = updates.get("district");
                registration.setDistrict(v != null && !String.valueOf(v).isBlank() ? String.valueOf(v).trim() : null);
            }
            if (updates.containsKey("ward")) {
                Object v = updates.get("ward");
                registration.setWard(v != null && !String.valueOf(v).isBlank() ? String.valueOf(v).trim() : null);
            }
            if (updates.containsKey("description") || updates.containsKey("registrationNote")) {
                Object v = updates.get("description") != null ? updates.get("description") : updates.get("registrationNote");
                registration.setRegistrationNote(v != null ? String.valueOf(v).trim() : null);
            }
        }

        FarmRegistration saved = farmRegistrationRepository.save(registration);
        return adminFarmRegMapper.toAdminResponse(saved);
    }

    @Override
    @Transactional
    public AdminFarmRegRespDTO handleRegistration(Integer adminUserId, Integer registrationId, HandleFarmRegReqDTO request) {
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản admin với ID: " + adminUserId));

        if (!"admin".equalsIgnoreCase(adminUser.getSystemRole())) {
            throw new ForbiddenException("Chỉ quản trị viên mới có quyền xét duyệt đơn đăng ký.");
        }

        FarmRegistration registration = farmRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn đăng ký trang trại với ID: " + registrationId));

        if (!"pending".equalsIgnoreCase(registration.getRegistrationStatus())) {
            throw new BadRequestException(
                    "Đơn đăng ký này đã được xử lý trước đó (trạng thái hiện tại: "
                            + registration.getRegistrationStatus() + "). Không thể xử lý lại.");
        }

        String action = request.action().toUpperCase();
        if ("APPROVED".equals(action)) {
            return handleApproval(registration, adminUser);
        } else {
            return handleRejection(registration, adminUser, request.rejectionReason());
        }
    }

    private AdminFarmRegRespDTO handleApproval(FarmRegistration registration, User adminUser) {
        Farm farm;
        boolean isUpdateRequest = registration.getResultingFarmId() != null;
        if (isUpdateRequest) {
            farm = farmRepository.findById(registration.getResultingFarmId()).orElseGet(Farm::new);
        } else {
            farm = new Farm();
            farm.setCreatedBy(registration.getUser());
        }
        farm.setFarmName(registration.getFarmName());
        farm.setAddress(registration.getAddress());
        String note = registration.getRegistrationNote();
        if (note != null) {
            note = note.replaceFirst("^\\[Cập nhật trang trại #\\d+\\]\\s*", "");
        }
        farm.setDescription(note);
        farm.setLatitude(registration.getLatitude());
        farm.setLongitude(registration.getLongitude());
        farm.setBoundaryCoordinates(registration.getBoundaryCoordinates());
        if (registration.getDocumentUrl() != null && !registration.getDocumentUrl().isBlank()) {
            farm.setFarmMapUrl(registration.getDocumentUrl());
        }
        farm.setFarmStatus("active");
        farm = farmRepository.save(farm);

        if (!isUpdateRequest || farmMemberRepository.findByFarmIdAndUserId(farm.getId(), registration.getUser().getId()).isEmpty()) {
            FarmMember ownerMember = new FarmMember(farm, registration.getUser(), "owner", "active");
            farmMemberRepository.save(ownerMember);
        }

        User applicant = registration.getUser();
        if (applicant != null && "user".equalsIgnoreCase(applicant.getSystemRole())) {
            applicant.setSystemRole("owner");
            userRepository.save(applicant);
        }

        registration.setRegistrationStatus("approved");
        registration.setReviewedBy(adminUser);
        registration.setReviewedAt(Instant.now());
        registration.setResultingFarmId(farm.getId());
        farmRegistrationRepository.save(registration);

        sendUserNotification(
                registration.getUser().getId(),
                "Đăng ký trang trại được phê duyệt",
                "Chúc mừng! Trang trại " + farm.getFarmName()
                        + " của bạn đã được phê duyệt. Bạn hiện có thể bắt đầu quản lý trang trại.",
                "FARM_REGISTRATION",
                "/dashboard");

        sendApprovalEmail(registration, farm);
        return adminFarmRegMapper.toAdminResponse(registration);
    }

    private AdminFarmRegRespDTO handleRejection(FarmRegistration registration, User adminUser, String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new BadRequestException("Vui lòng cung cấp lý do từ chối đơn đăng ký.");
        }
        String trimmedReason = rejectionReason.trim();
        if (trimmedReason.length() > 1000) {
            throw new BadRequestException("Lý do từ chối không được vượt quá 1000 ký tự.");
        }

        registration.setRegistrationStatus("rejected");
        registration.setReviewedBy(adminUser);
        registration.setReviewedAt(Instant.now());
        registration.setRejectionReason(trimmedReason);
        farmRegistrationRepository.save(registration);

        sendUserNotification(
                registration.getUser().getId(),
                "Đăng ký trang trại bị từ chối",
                "Yêu cầu đăng ký trang trại " + registration.getFarmName()
                        + " đã bị từ chối. Lý do: " + trimmedReason,
                "FARM_REGISTRATION",
                "/farm-registrations/" + registration.getId());

        sendRejectionEmail(registration, trimmedReason);
        return adminFarmRegMapper.toAdminResponse(registration);
    }

    private void sendUserNotification(Integer userId, String title, String message, String type, String actionUrl) {
        if (notificationService == null || userId == null) {
            return;
        }
        try {
            notificationService.createNotification(userId, title, message, type, actionUrl);
        } catch (Exception e) {
            log.warn("Không thể tạo notification cho userId {}: {}", userId, e.getMessage());
        }
    }

    private void sendApprovalEmail(FarmRegistration registration, Farm farm) {
        if (mailSender == null) return;
        try {
            User applicant = registration.getUser();
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            if (mimeMessage == null) mimeMessage = new JavaMailSenderImpl().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            if (mailFrom != null && !mailFrom.isBlank()) helper.setFrom(mailFrom, "MyCropDiary");
            helper.setTo(applicant.getEmail());
            helper.setSubject("[MyCropDiary] Chúc mừng! Đơn đăng ký trang trại của bạn đã được phê duyệt");
            helper.setText(EmailTemplateBuilder.buildFarmApprovedEmail(applicant.getFullName(), farm.getFarmName(), farm.getFarmCode()), true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            log.warn("Không thể gửi email thông báo duyệt trang trại tới {}: {}", registration.getUser().getEmail(), e.getMessage());
        }
    }

    private void sendRejectionEmail(FarmRegistration registration, String rejectionReason) {
        if (mailSender == null) return;
        try {
            User applicant = registration.getUser();
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            if (mimeMessage == null) mimeMessage = new JavaMailSenderImpl().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            if (mailFrom != null && !mailFrom.isBlank()) helper.setFrom(mailFrom, "MyCropDiary");
            helper.setTo(applicant.getEmail());
            helper.setSubject("[MyCropDiary] Kết quả thẩm định đơn đăng ký trang trại");
            helper.setText(EmailTemplateBuilder.buildFarmRejectedEmail(applicant.getFullName(), registration.getFarmName(), rejectionReason), true);
            mailSender.send(mimeMessage);
        } catch (Exception e) {
            log.warn("Không thể gửi email thông báo từ chối trang trại tới {}: {}", registration.getUser().getEmail(), e.getMessage());
        }
    }
}
