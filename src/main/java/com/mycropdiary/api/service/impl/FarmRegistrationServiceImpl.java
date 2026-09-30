package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.farm.FarmRegistrationResponse;
import com.mycropdiary.api.dto.farm.HandleRegistrationRequest;
import com.mycropdiary.api.entity.AppUser;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.farmregistration.FarmRegistration;
import com.mycropdiary.api.entity.farmregistration.FarmRegistrationStatus;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.AppUserRepository;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.farmregistration.FarmRegistrationRepository;
import com.mycropdiary.api.service.FarmRegistrationService;
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

// [AI_CHANGE] Root cause: UC-39 cần triển khai nghiệp vụ Admin duyệt/từ chối đơn đăng ký trang trại
// [AI_CHANGE] Mechanism: Khi APPROVED -> tạo Farm + FarmMember OWNER trong cùng transaction;
//             Khi REJECTED -> cập nhật status + rejectionReason; Chặn xử lý trùng bằng kiểm tra PENDING
@Service
@Transactional(readOnly = true)
public class FarmRegistrationServiceImpl implements FarmRegistrationService {
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

    // ==================== UC-39: Danh sách đơn đăng ký ====================
    @Override
    public PageResponse<FarmRegistrationResponse> getRegistrations(String status, Pageable pageable) {
        Page<FarmRegistration> page;

        // [AI_CHANGE] Root cause: Admin cần lọc theo trạng thái hoặc xem tất cả
        // [AI_CHANGE] Mechanism: Nếu status null -> findAll, ngược lại parse enum và findByStatus
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

        return PageResponse.map(page, this::toResponse);
    }

    // ==================== UC-39: Chi tiết đơn đăng ký ====================
    @Override
    public FarmRegistrationResponse getRegistrationById(Long registrationId) {
        FarmRegistration registration = farmRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn đăng ký trang trại với ID: " + registrationId));
        return toResponse(registration);
    }

    // ==================== UC-39: Duyệt / Từ chối ====================
    @Override
    @Transactional
    public FarmRegistrationResponse handleRegistration(Long adminUserId, Long registrationId,
                                                        HandleRegistrationRequest request) {
        // [AI_CHANGE] Root cause: Cần lấy thông tin admin đang xử lý để ghi nhận handlerUser
        AppUser adminUser = appUserRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản admin với ID: " + adminUserId));

        FarmRegistration registration = farmRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy đơn đăng ký trang trại với ID: " + registrationId));

        // [AI_CHANGE] Root cause: Chặn xử lý trùng – chỉ đơn PENDING mới được xử lý
        // [AI_CHANGE] Mechanism: Kiểm tra status hiện tại, nếu không phải PENDING thì từ chối hành động
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

    // ==================== Private: Xử lý duyệt ====================
    private FarmRegistrationResponse handleApproval(FarmRegistration registration, AppUser adminUser) {
        // [AI_CHANGE] Root cause: Khi duyệt cần tạo Farm mới + gán OWNER cho người nộp đơn
        // [AI_CHANGE] Mechanism: Sinh FarmCode duy nhất, tạo Farm entity, tạo FarmMember OWNER, cập nhật status

        // Sinh mã trang trại duy nhất
        String farmCode = generateUniqueFarmCode();

        // Tạo Farm từ thông tin đơn đăng ký
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

        // [AI_CHANGE] Gán người nộp đơn làm OWNER của trang trại mới
        FarmMember ownerMember = new FarmMember(
                farm,
                registration.getApplicantUser(),
                "OWNER",
                "Chủ trang trại",
                LocalDate.now(),
                "ACTIVE"
        );
        farmMemberRepository.save(ownerMember);

        // Cập nhật trạng thái đơn đăng ký
        registration.setStatus(FarmRegistrationStatus.APPROVED);
        registration.setHandlerUser(adminUser);
        registration.setHandledAt(LocalDateTime.now());
        farmRegistrationRepository.save(registration);

        log.info("UC-39: Admin {} đã DUYỆT đơn đăng ký #{} -> Farm '{}' (Code: {}) đã được tạo, OWNER: {}",
                adminUser.getEmail(), registration.getId(), farm.getFarmName(), farmCode,
                registration.getApplicantUser().getEmail());

        // [AI_CHANGE] Gửi email chúc mừng và cung cấp thông tin trang trại cho người nộp đơn
        sendApprovalEmail(registration, farm);

        return toResponse(registration);
    }

    // ==================== Private: Xử lý từ chối ====================
    private FarmRegistrationResponse handleRejection(FarmRegistration registration, AppUser adminUser,
                                                      String rejectionReason) {
        // [AI_CHANGE] Root cause: Khi từ chối cần bắt buộc có lý do
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

        // [AI_CHANGE] Gửi email thông báo từ chối kèm lý do thẩm định cụ thể cho người nộp đơn
        sendRejectionEmail(registration, rejectionReason.trim());

        return toResponse(registration);
    }

    // ==================== Private: Email Notifications ====================
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

    // ==================== Private helpers ====================

    // [AI_CHANGE] Root cause: FarmCode phải unique, cần cơ chế sinh mã không trùng
    // [AI_CHANGE] Mechanism: Dùng prefix "FARM-" + 8 ký tự UUID uppercase, kiểm tra trùng lặp
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

    // [AI_CHANGE] Mapper thủ công từ Entity sang DTO Response
    private FarmRegistrationResponse toResponse(FarmRegistration reg) {
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
                reg.getStatus().name(),
                handler != null ? handler.getId() : null,
                handler != null ? handler.getFullName() : null,
                reg.getSubmittedAt(),
                reg.getHandledAt(),
                reg.getRejectionReason()
        );
    }
}
