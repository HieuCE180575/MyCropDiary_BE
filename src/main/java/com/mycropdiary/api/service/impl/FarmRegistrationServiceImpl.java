package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationReqDTO;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationRespDTO;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationSummaryRespDTO;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.farmregistration.FarmRegistration;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.farmregistration.FarmRegistrationRepository;
import com.mycropdiary.api.service.NotificationService;
import com.mycropdiary.api.mapper.FarmRegistrationMapper;
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
import java.util.List;

/**
 * Service implementation cho cả nghiệp vụ Admin duyệt đơn (UC-39) và User đăng
 * ký trang trại (UC-09).
 */
@Service
@Transactional(readOnly = true)
public class FarmRegistrationServiceImpl implements FarmRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(FarmRegistrationServiceImpl.class);

    private final FarmRegistrationRepository farmRegistrationRepository;
    private final FarmRepository farmRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    public FarmRegistrationServiceImpl(
            FarmRegistrationRepository farmRegistrationRepository,
            UserRepository userRepository,
            FarmRepository farmRepository,
            FarmMemberRepository farmMemberRepository,
            NotificationService notificationService) {
        this.farmRegistrationRepository = farmRegistrationRepository;
        this.userRepository = userRepository;
        this.farmRepository = farmRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.notificationService = notificationService;
    }

    private User validateAndGetActiveUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản người dùng không tồn tại."));
        if (!user.isActive() || user.isLocked()) {
            throw new ForbiddenException("Tài khoản chưa được kích hoạt hoặc đã bị khóa.");
        }
        return user;
    }

    private void notifyAdmins(String title, String message, String actionUrl) {
        if (notificationService == null) {
            return;
        }
        List<User> admins = userRepository.findAllBySystemRoleIgnoreCaseAndAccountStatusIgnoreCase(
                "admin", "active");

        for (User admin : admins) {
            notificationService.createNotification(
                    admin.getId(),
                    title,
                    message,
                    "FARM_REGISTRATION",
                    actionUrl);
        }
    }

    private void sendUserNotification(Integer userId, String title, String message, String type, String actionUrl) {
        if (notificationService != null) {
            notificationService.createNotification(userId, title, message, type, actionUrl);
        }
    }
    // ==================== UC-09: User side APIs ====================

    @Override
    @Transactional
    public com.mycropdiary.api.dto.farmregistration.FarmRegistrationRespDTO createRegistration(
            Integer currentUserId, FarmRegistrationReqDTO request) {
        User applicantUser = validateAndGetActiveUser(currentUserId);

        if (request.boundaryCoordinates() != null && !request.boundaryCoordinates().isBlank()) {
            double areaM2 = com.mycropdiary.api.util.GeoAreaUtils.calculatePolygonAreaM2(request.boundaryCoordinates());
            if (areaM2 <= 0.0) {
                throw new com.mycropdiary.api.exception.BusinessRuleException(
                        "Diện tích vùng ranh giới trang trại trên bản đồ phải lớn hơn 0 m² (cần ít nhất 3 điểm hợp lệ).");
            }
        }

        FarmRegistration registration = new FarmRegistration();
        registration.setUser(applicantUser);
        registration.setFarmName(request.farmName().trim());
        registration.setAddress(request.addressLine().trim());
        registration.setProvince(
                request.province() != null && !request.province().isBlank() ? request.province().trim() : null);
        registration.setDistrict(
                request.district() != null && !request.district().isBlank() ? request.district().trim() : null);
        registration.setWard(request.ward() != null && !request.ward().isBlank() ? request.ward().trim() : null);
        registration.setRegistrationNote(
                request.description() != null && !request.description().isBlank() ? request.description().trim()
                        : null);
        registration.setDocumentUrl(
                request.documentUrl() != null && !request.documentUrl().isBlank() ? request.documentUrl().trim()
                        : null);
        registration.setLatitude(request.latitude());
        registration.setLongitude(request.longitude());
        registration.setBoundaryCoordinates(
                request.boundaryCoordinates() != null && !request.boundaryCoordinates().isBlank()
                        ? request.boundaryCoordinates().trim()
                        : null);
        registration.setRegistrationStatus("pending");
        registration.setReviewedBy(null);
        registration.setReviewedAt(null);

        FarmRegistration saved = farmRegistrationRepository.save(registration);

        // Notify all administrators
        notifyAdmins(
                "New farm registration",
                "A new farm registration has been submitted and requires your review.",
                "/admin/farm-registrations");

        // Notify applicant that registration was submitted
        sendUserNotification(
                currentUserId,
                "Đăng ký trang trại thành công",
                "Yêu cầu đăng ký trang trại " + saved.getFarmName()
                        + " đã được gửi và đang chờ quản trị viên xét duyệt.",
                "FARM_REGISTRATION",
                "/farm-registrations/" + saved.getId());

        return mapToUserResponse(saved);
    }

    @Override
    public PageResp<FarmRegistrationSummaryRespDTO> getMyRegistrations(Integer currentUserId, Pageable pageable) {
        validateAndGetActiveUser(currentUserId);
        Page<FarmRegistration> page = farmRegistrationRepository.findByApplicantUserId(currentUserId, pageable);
        return PageResp.map(page, this::mapToSummaryResponse);
    }

    @Override
    public com.mycropdiary.api.dto.farmregistration.FarmRegistrationRespDTO getMyRegistrationDetail(
            Integer currentUserId, Integer registrationId) {
        validateAndGetActiveUser(currentUserId);
        FarmRegistration registration = farmRegistrationRepository
                .findByIdAndApplicantUserId(registrationId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu đăng ký trang trại không tồn tại."));
        return mapToUserResponse(registration);
    }

    @Override
    @Transactional
    public com.mycropdiary.api.dto.farmregistration.FarmRegistrationRespDTO cancelRegistration(
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

        // Notify applicant that registration was cancelled
        sendUserNotification(
                currentUserId,
                "Đã hủy yêu cầu đăng ký",
                "Yêu cầu đăng ký trang trại " + updated.getFarmName()
                        + " đã được hủy thành công.",
                "FARM_REGISTRATION",
                "/farm-registrations/" + updated.getId());

        notifyAdmins(
                "Farm registration cancelled",
                "A user has cancelled a pending farm registration.",
                "/admin/farm-registrations");

        return mapToUserResponse(updated);
    }

    private final FarmRegistrationMapper farmRegistrationMapper = new FarmRegistrationMapper();

    private FarmRegistrationRespDTO mapToUserResponse(FarmRegistration reg) {
        return farmRegistrationMapper.toUserResponse(reg);
    }

    private FarmRegistrationSummaryRespDTO mapToSummaryResponse(FarmRegistration reg) {
        return farmRegistrationMapper.toSummaryResponse(reg);
    }
}
