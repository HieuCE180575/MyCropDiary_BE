package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.farm.FarmRegistrationResp;
import com.mycropdiary.api.dto.farm.HandleRegistrationReq;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationCreateReq;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationSummaryResp;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.farmregistration.FarmRegistration;
import com.mycropdiary.api.entity.farmregistration.FarmRegistrationStatus;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.farmregistration.FarmRegistrationRepository;
import com.mycropdiary.api.service.farmregistration.FarmRegistrationService;
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
import java.time.LocalDateTime;

/**
 * Service implementation cho cả nghiệp vụ Admin duyệt đơn (UC-39) và User đăng ký trang trại (UC-09).
 */
@Service
@Transactional(readOnly = true)
public class FarmRegistrationServiceImpl implements FarmRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(FarmRegistrationServiceImpl.class);

    private final FarmRegistrationRepository farmRegistrationRepository;
    private final FarmRepository farmRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final UserRepository userRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public FarmRegistrationServiceImpl(FarmRegistrationRepository farmRegistrationRepository,
                                       FarmRepository farmRepository,
                                       FarmMemberRepository farmMemberRepository,
                                       UserRepository userRepository) {
        this.farmRegistrationRepository = farmRegistrationRepository;
        this.farmRepository = farmRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.userRepository = userRepository;
    }

    private User validateAndGetActiveUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản người dùng không tồn tại."));
        if (!user.isActive() || user.isLocked()) {
            throw new ForbiddenException("Tài khoản chưa được kích hoạt hoặc đã bị khóa.");
        }
        return user;
    }

    // ==================== UC-09: User side APIs ====================

    @Override
    @Transactional
    public com.mycropdiary.api.dto.farmregistration.FarmRegistrationResp createRegistration(
            Integer currentUserId, FarmRegistrationCreateReq request) {
        User applicantUser = validateAndGetActiveUser(currentUserId);

        FarmRegistration registration = new FarmRegistration();
        registration.setUser(applicantUser);
        registration.setFarmName(request.farmName().trim());
        registration.setAddress(request.addressLine().trim());
        registration.setProvince(request.province() != null && !request.province().isBlank() ? request.province().trim() : null);
        registration.setDistrict(request.district() != null && !request.district().isBlank() ? request.district().trim() : null);
        registration.setWard(request.ward() != null && !request.ward().isBlank() ? request.ward().trim() : null);
        registration.setRegistrationNote(request.description() != null && !request.description().isBlank() ? request.description().trim() : null);
        registration.setDocumentUrl(request.documentUrl() != null && !request.documentUrl().isBlank() ? request.documentUrl().trim() : null);
        registration.setRegistrationStatus("pending");
        registration.setReviewedBy(null);
        registration.setReviewedAt(null);

        FarmRegistration saved = farmRegistrationRepository.save(registration);
        return mapToUserResponse(saved);
    }

    @Override
    public PageResp<FarmRegistrationSummaryResp> getMyRegistrations(Integer currentUserId, Pageable pageable) {
        validateAndGetActiveUser(currentUserId);
        Page<FarmRegistration> page = farmRegistrationRepository.findByApplicantUserId(currentUserId, pageable);
        return PageResp.map(page, this::mapToSummaryResponse);
    }

    @Override
    public com.mycropdiary.api.dto.farmregistration.FarmRegistrationResp getMyRegistrationDetail(
            Integer currentUserId, Integer registrationId) {
        validateAndGetActiveUser(currentUserId);
        FarmRegistration registration = farmRegistrationRepository
                .findByIdAndApplicantUserId(registrationId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu đăng ký trang trại không tồn tại."));
        return mapToUserResponse(registration);
    }

    @Override
    @Transactional
    public com.mycropdiary.api.dto.farmregistration.FarmRegistrationResp cancelRegistration(
            Integer currentUserId, Integer registrationId) {
        validateAndGetActiveUser(currentUserId);
        FarmRegistration registration = farmRegistrationRepository
                .findByIdAndApplicantUserId(registrationId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu đăng ký trang trại không tồn tại."));

        if ("cancelled".equalsIgnoreCase(registration.getRegistrationStatus())) {
            throw new BadRequestException("Yêu cầu đăng ký trang trại đã bị hủy trước đó.");
        }
        if ("approved".equalsIgnoreCase(registration.getRegistrationStatus())) {
            throw new BadRequestException("Không thể hủy yêu cầu đăng ký trang trại đã được duyệt.");
        }
        if ("rejected".equalsIgnoreCase(registration.getRegistrationStatus())) {
            throw new BadRequestException("Không thể hủy yêu cầu đăng ký trang trại đã bị từ chối.");
        }
        if (!"pending".equalsIgnoreCase(registration.getRegistrationStatus())) {
            throw new BadRequestException("Chỉ có thể hủy yêu cầu đăng ký trang trại đang ở trạng thái PENDING.");
        }

        registration.setRegistrationStatus("cancelled");
        FarmRegistration updated = farmRegistrationRepository.save(registration);
        return mapToUserResponse(updated);
    }

    // ==================== UC-39: Admin side APIs ====================

    @Override
    public PageResp<FarmRegistrationResp> getRegistrations(String status, Pageable pageable) {
        Page<FarmRegistration> page;
        if (status != null && !status.isBlank()) {
            page = farmRegistrationRepository.findByStatusString(status.trim().toLowerCase(), pageable);
        } else {
            page = farmRegistrationRepository.findAll(pageable);
        }

        return PageResp.map(page, this::toAdminResponse);
    }

    @Override
    public FarmRegistrationResp getRegistrationById(Integer registrationId) {
        FarmRegistration registration = farmRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn đăng ký trang trại với ID: " + registrationId));
        return toAdminResponse(registration);
    }

    @Override
    @Transactional
    public FarmRegistrationResp handleRegistration(Integer adminUserId, Integer registrationId,
                                                        HandleRegistrationReq request) {
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản admin với ID: " + adminUserId));

        FarmRegistration registration = farmRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn đăng ký trang trại với ID: " + registrationId));

        if (!"pending".equalsIgnoreCase(registration.getRegistrationStatus())) {
            throw new BadRequestException(
                    "Đơn đăng ký này đã được xử lý trước đó (trạng thái hiện tại: " + registration.getRegistrationStatus() + "). Không thể xử lý lại.");
        }

        String action = request.action().toUpperCase();

        if ("APPROVED".equals(action)) {
            return handleApproval(registration, adminUser);
        } else {
            return handleRejection(registration, adminUser, request.rejectionReason());
        }
    }

    private FarmRegistrationResp handleApproval(FarmRegistration registration, User adminUser) {
        Farm farm = new Farm();
        farm.setFarmName(registration.getFarmName());
        farm.setAddress(registration.getAddress());
        farm.setDescription(registration.getRegistrationNote());
        farm.setCreatedBy(registration.getUser());
        farm.setFarmStatus("active");
        farm = farmRepository.save(farm);

        FarmMember ownerMember = new FarmMember(
                farm,
                registration.getUser(),
                "owner",
                "active"
        );
        farmMemberRepository.save(ownerMember);

        registration.setRegistrationStatus("approved");
        registration.setReviewedBy(adminUser);
        registration.setReviewedAt(Instant.now());
        registration.setResultingFarmId(farm.getId());
        farmRegistrationRepository.save(registration);

        log.info("UC-39: Admin {} đã DUYỆT đơn đăng ký #{} -> Farm '{}' đã được tạo, OWNER: {}",
                adminUser.getEmail(), registration.getId(), farm.getFarmName(),
                registration.getUser().getEmail());

        sendApprovalEmail(registration, farm);

        return toAdminResponse(registration);
    }

    private FarmRegistrationResp handleRejection(FarmRegistration registration, User adminUser,
                                                      String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new BadRequestException("Vui lòng cung cấp lý do từ chối đơn đăng ký.");
        }

        registration.setRegistrationStatus("rejected");
        registration.setReviewedBy(adminUser);
        registration.setReviewedAt(Instant.now());
        registration.setRegistrationNote(rejectionReason.trim());
        farmRegistrationRepository.save(registration);

        log.info("UC-39: Admin {} đã TỪ CHỐI đơn đăng ký #{} với lý do: {}",
                adminUser.getEmail(), registration.getId(), rejectionReason);

        sendRejectionEmail(registration, rejectionReason.trim());

        return toAdminResponse(registration);
    }

    private void sendApprovalEmail(FarmRegistration registration, Farm farm) {
        if (mailSender == null) return;
        try {
            User applicant = registration.getUser();
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            if (mimeMessage == null) {
                mimeMessage = new JavaMailSenderImpl().createMimeMessage();
            }
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom, "MyCropDiary");
            }
            helper.setTo(applicant.getEmail());
            helper.setSubject("[MyCropDiary] Chúc mừng! Đơn đăng ký trang trại của bạn đã được phê duyệt");
            String htmlContent = EmailTemplateBuilder.buildFarmApprovedEmail(
                    applicant.getFullName(), farm.getFarmName(), farm.getFarmCode());
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Approval notification email sent to: {}", applicant.getEmail());
        } catch (Exception e) {
            log.warn("Không thể gửi email thông báo duyệt trang trại tới {}: {}",
                    registration.getUser().getEmail(), e.getMessage());
        }
    }

    private void sendRejectionEmail(FarmRegistration registration, String rejectionReason) {
        if (mailSender == null) return;
        try {
            User applicant = registration.getUser();
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            if (mimeMessage == null) {
                mimeMessage = new JavaMailSenderImpl().createMimeMessage();
            }
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            if (mailFrom != null && !mailFrom.isBlank()) {
                helper.setFrom(mailFrom, "MyCropDiary");
            }
            helper.setTo(applicant.getEmail());
            helper.setSubject("[MyCropDiary] Kết quả thẩm định đơn đăng ký trang trại");
            String htmlContent = EmailTemplateBuilder.buildFarmRejectedEmail(
                    applicant.getFullName(), registration.getFarmName(), rejectionReason);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Rejection notification email sent to: {}", applicant.getEmail());
        } catch (Exception e) {
            log.warn("Không thể gửi email thông báo từ chối trang trại tới {}: {}",
                    registration.getUser().getEmail(), e.getMessage());
        }
    }

    private FarmRegistrationResp toAdminResponse(FarmRegistration reg) {
        User applicant = reg.getUser();
        User handler = reg.getReviewedBy();

        return new FarmRegistrationResp(
                reg.getId(),
                applicant != null ? applicant.getId() : null,
                applicant != null ? applicant.getEmail() : null,
                applicant != null ? applicant.getFullName() : null,
                reg.getFarmName(),
                reg.getAddress(),
                reg.getProvince(),
                reg.getDistrict(),
                reg.getWard(),
                reg.getDescription(),
                reg.getDocumentUrl(),
                reg.getRegistrationStatus() != null ? reg.getRegistrationStatus().toUpperCase() : null,
                handler != null ? handler.getId() : null,
                handler != null ? handler.getFullName() : null,
                reg.getSubmittedAt(),
                reg.getHandledAt(),
                reg.getRejectionReason()
        );
    }

    private com.mycropdiary.api.dto.farmregistration.FarmRegistrationResp mapToUserResponse(FarmRegistration reg) {
        return new com.mycropdiary.api.dto.farmregistration.FarmRegistrationResp(
                reg.getId(),
                reg.getFarmName(),
                reg.getAddress(),
                reg.getProvince(),
                reg.getDistrict(),
                reg.getWard(),
                reg.getDescription(),
                reg.getDocumentUrl(),
                reg.getStatus(),
                reg.getSubmittedAt(),
                reg.getHandledAt(),
                reg.getRejectionReason()
        );
    }

    private FarmRegistrationSummaryResp mapToSummaryResponse(FarmRegistration reg) {
        return new FarmRegistrationSummaryResp(
                reg.getId(),
                reg.getFarmName(),
                reg.getAddress(),
                reg.getProvince(),
                reg.getDistrict(),
                reg.getWard(),
                reg.getStatus(),
                reg.getSubmittedAt(),
                reg.getHandledAt(),
                reg.getRejectionReason()
        );
    }
}
