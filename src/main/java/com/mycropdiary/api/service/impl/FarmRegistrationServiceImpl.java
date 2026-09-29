package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.farm.FarmRegistrationResponse;
import com.mycropdiary.api.dto.farm.HandleRegistrationRequest;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationCreateRequest;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationSummaryResponse;
import com.mycropdiary.api.entity.AppUser;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.farmregistration.FarmRegistration;
import com.mycropdiary.api.entity.farmregistration.FarmRegistrationStatus;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.AppUserRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.farmregistration.FarmRegistrationRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service implementation cho cả nghiệp vụ Admin duyệt đơn (UC-39) và User đăng ký trang trại (UC-09).
 */
@Service
@Transactional(readOnly = true)
public class FarmRegistrationServiceImpl implements 
        com.mycropdiary.api.service.FarmRegistrationService,
        com.mycropdiary.api.service.farmregistration.FarmRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(FarmRegistrationServiceImpl.class);

    private final FarmRegistrationRepository farmRegistrationRepository;
    private final FarmRepository farmRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final AppUserRepository appUserRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public FarmRegistrationServiceImpl(FarmRegistrationRepository farmRegistrationRepository,
                                       FarmRepository farmRepository,
                                       FarmMemberRepository farmMemberRepository,
                                       AppUserRepository appUserRepository) {
        this.farmRegistrationRepository = farmRegistrationRepository;
        this.farmRepository = farmRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.appUserRepository = appUserRepository;
    }

    private AppUser validateAndGetActiveUser(Long userId) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản người dùng không tồn tại."));
        if (!user.isActive() || user.isLocked()) {
            throw new ForbiddenException("Tài khoản chưa được kích hoạt hoặc đã bị khóa.");
        }
        return user;
    }

    // ==================== UC-09: User side APIs ====================

    @Override
    @Transactional
    public com.mycropdiary.api.dto.farmregistration.FarmRegistrationResponse createRegistration(
            Long currentUserId, FarmRegistrationCreateRequest request) {
        AppUser applicantUser = validateAndGetActiveUser(currentUserId);

        FarmRegistration registration = new FarmRegistration();
        registration.setApplicantUser(applicantUser);
        registration.setFarmName(request.farmName().trim());
        registration.setAddressLine(request.addressLine().trim());
        registration.setProvince(request.province() != null ? request.province().trim() : null);
        registration.setDistrict(request.district() != null ? request.district().trim() : null);
        registration.setWard(request.ward() != null ? request.ward().trim() : null);
        registration.setDescription(request.description() != null ? request.description().trim() : null);
        registration.setDocumentUrl(request.documentUrl() != null ? request.documentUrl().trim() : null);
        registration.setStatus(FarmRegistrationStatus.PENDING);
        registration.setHandlerUser(null);
        registration.setHandledAt(null);
        registration.setRejectionReason(null);

        FarmRegistration saved = farmRegistrationRepository.save(registration);
        return mapToUserResponse(saved);
    }

    @Override
    public PageResponse<FarmRegistrationSummaryResponse> getMyRegistrations(Long currentUserId, Pageable pageable) {
        validateAndGetActiveUser(currentUserId);
        Page<FarmRegistration> page = farmRegistrationRepository.findByApplicantUserId(currentUserId, pageable);
        return PageResponse.map(page, this::mapToSummaryResponse);
    }

    @Override
    public com.mycropdiary.api.dto.farmregistration.FarmRegistrationResponse getMyRegistrationDetail(
            Long currentUserId, Long registrationId) {
        validateAndGetActiveUser(currentUserId);
        FarmRegistration registration = farmRegistrationRepository
                .findByIdAndApplicantUserId(registrationId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu đăng ký trang trại không tồn tại."));
        return mapToUserResponse(registration);
    }

    @Override
    @Transactional
    public com.mycropdiary.api.dto.farmregistration.FarmRegistrationResponse cancelRegistration(
            Long currentUserId, Long registrationId) {
        validateAndGetActiveUser(currentUserId);
        FarmRegistration registration = farmRegistrationRepository
                .findByIdAndApplicantUserId(registrationId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu đăng ký trang trại không tồn tại."));

        if (registration.getStatus() == FarmRegistrationStatus.CANCELLED) {
            throw new BadRequestException("Yêu cầu đăng ký trang trại đã bị hủy trước đó.");
        }
        if (registration.getStatus() == FarmRegistrationStatus.APPROVED) {
            throw new BadRequestException("Không thể hủy yêu cầu đăng ký trang trại đã được duyệt.");
        }
        if (registration.getStatus() == FarmRegistrationStatus.REJECTED) {
            throw new BadRequestException("Không thể hủy yêu cầu đăng ký trang trại đã bị từ chối.");
        }
        if (registration.getStatus() != FarmRegistrationStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể hủy yêu cầu đăng ký trang trại đang ở trạng thái PENDING.");
        }

        registration.setStatus(FarmRegistrationStatus.CANCELLED);
        FarmRegistration updated = farmRegistrationRepository.save(registration);
        return mapToUserResponse(updated);
    }

    // ==================== UC-39: Admin side APIs ====================

    @Override
    public PageResponse<FarmRegistrationResponse> getRegistrations(String status, Pageable pageable) {
        Page<FarmRegistration> page;
        if (status != null && !status.isBlank()) {
            FarmRegistrationStatus registrationStatus;
            try {
                registrationStatus = FarmRegistrationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Trạng thái không hợp lệ: " + status + ". Các giá trị hợp lệ: PENDING, APPROVED, REJECTED, CANCELLED");
            }
            page = farmRegistrationRepository.findByStatus(registrationStatus, pageable);
        } else {
            page = farmRegistrationRepository.findAll(pageable);
        }

        return PageResponse.map(page, this::toAdminResponse);
    }

    @Override
    public FarmRegistrationResponse getRegistrationById(Long registrationId) {
        FarmRegistration registration = farmRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn đăng ký trang trại với ID: " + registrationId));
        return toAdminResponse(registration);
    }

    @Override
    @Transactional
    public FarmRegistrationResponse handleRegistration(Long adminUserId, Long registrationId,
                                                        HandleRegistrationRequest request) {
        AppUser adminUser = appUserRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản admin với ID: " + adminUserId));

        FarmRegistration registration = farmRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn đăng ký trang trại với ID: " + registrationId));

        if (registration.getStatus() != FarmRegistrationStatus.PENDING) {
            throw new BadRequestException(
                    "Đơn đăng ký này đã được xử lý trước đó (trạng thái hiện tại: " + registration.getStatus() + "). Không thể xử lý lại.");
        }

        FarmRegistrationStatus action = FarmRegistrationStatus.valueOf(request.action());

        if (action == FarmRegistrationStatus.APPROVED) {
            return handleApproval(registration, adminUser);
        } else {
            return handleRejection(registration, adminUser, request.rejectionReason());
        }
    }

    private FarmRegistrationResponse handleApproval(FarmRegistration registration, AppUser adminUser) {
        String farmCode = generateUniqueFarmCode();

        Farm farm = new Farm();
        farm.setFarmCode(farmCode);
        farm.setFarmName(registration.getFarmName());
        farm.setAddressLine(registration.getAddressLine());
        farm.setProvince(registration.getProvince());
        farm.setDistrict(registration.getDistrict());
        farm.setWard(registration.getWard());
        farm.setRegistrationId(registration.getId());
        farm.setStatus("ACTIVE");
        farm = farmRepository.save(farm);

        FarmMember ownerMember = new FarmMember(
                farm,
                registration.getApplicantUser(),
                "OWNER",
                "Chủ trang trại",
                LocalDate.now(),
                "ACTIVE"
        );
        farmMemberRepository.save(ownerMember);

        registration.setStatus(FarmRegistrationStatus.APPROVED);
        registration.setHandlerUser(adminUser);
        registration.setHandledAt(LocalDateTime.now());
        farmRegistrationRepository.save(registration);

        log.info("UC-39: Admin {} đã DUYỆT đơn đăng ký #{} -> Farm '{}' (Code: {}) đã được tạo, OWNER: {}",
                adminUser.getEmail(), registration.getId(), farm.getFarmName(), farmCode,
                registration.getApplicantUser().getEmail());

        sendApprovalEmail(registration, farm);

        return toAdminResponse(registration);
    }

    private FarmRegistrationResponse handleRejection(FarmRegistration registration, AppUser adminUser,
                                                      String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new BadRequestException("Vui lòng cung cấp lý do từ chối đơn đăng ký.");
        }

        registration.setStatus(FarmRegistrationStatus.REJECTED);
        registration.setHandlerUser(adminUser);
        registration.setHandledAt(LocalDateTime.now());
        registration.setRejectionReason(rejectionReason.trim());
        farmRegistrationRepository.save(registration);

        log.info("UC-39: Admin {} đã TỪ CHỐI đơn đăng ký #{} với lý do: {}",
                adminUser.getEmail(), registration.getId(), rejectionReason);

        sendRejectionEmail(registration, rejectionReason.trim());

        return toAdminResponse(registration);
    }

    private void sendApprovalEmail(FarmRegistration registration, Farm farm) {
        if (mailSender == null) return;
        try {
            AppUser applicant = registration.getApplicantUser();
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
                    registration.getApplicantUser().getEmail(), e.getMessage());
        }
    }

    private void sendRejectionEmail(FarmRegistration registration, String rejectionReason) {
        if (mailSender == null) return;
        try {
            AppUser applicant = registration.getApplicantUser();
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
                    registration.getApplicantUser().getEmail(), e.getMessage());
        }
    }

    private String generateUniqueFarmCode() {
        String code;
        int maxAttempts = 10;
        int attempt = 0;
        do {
            code = "FARM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            attempt++;
            if (attempt > maxAttempts) {
                throw new IllegalStateException("Không thể sinh mã trang trại duy nhất sau " + maxAttempts + " lần thử.");
            }
        } while (farmRepository.existsByFarmCode(code));
        return code;
    }

    private FarmRegistrationResponse toAdminResponse(FarmRegistration reg) {
        AppUser applicant = reg.getApplicantUser();
        AppUser handler = reg.getHandlerUser();

        return new FarmRegistrationResponse(
                reg.getId(),
                applicant != null ? applicant.getId() : null,
                applicant != null ? applicant.getEmail() : null,
                applicant != null ? applicant.getFullName() : null,
                reg.getFarmName(),
                reg.getAddressLine(),
                reg.getProvince(),
                reg.getDistrict(),
                reg.getWard(),
                reg.getDescription(),
                reg.getDocumentUrl(),
                reg.getStatus() != null ? reg.getStatus().name() : null,
                handler != null ? handler.getId() : null,
                handler != null ? handler.getFullName() : null,
                reg.getSubmittedAt(),
                reg.getHandledAt(),
                reg.getRejectionReason()
        );
    }

    private com.mycropdiary.api.dto.farmregistration.FarmRegistrationResponse mapToUserResponse(FarmRegistration reg) {
        return new com.mycropdiary.api.dto.farmregistration.FarmRegistrationResponse(
                reg.getId(),
                reg.getFarmName(),
                reg.getAddressLine(),
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

    private FarmRegistrationSummaryResponse mapToSummaryResponse(FarmRegistration reg) {
        return new FarmRegistrationSummaryResponse(
                reg.getId(),
                reg.getFarmName(),
                reg.getAddressLine(),
                reg.getStatus(),
                reg.getSubmittedAt(),
                reg.getHandledAt(),
                reg.getRejectionReason()
        );
    }
}
