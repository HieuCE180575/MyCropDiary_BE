package com.mycropdiary.api.dto.farmregistration;

import com.mycropdiary.api.entity.farmregistration.FarmRegistrationStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO phản hồi chi tiết thông tin đơn đăng ký trang trại.
 */
public record FarmRegistrationResponse(
        UUID registrationId,
        String farmName,
        String addressLine,
        String province,
        String district,
        String ward,
        String description,
        String documentUrl,
        FarmRegistrationStatus status,
        LocalDateTime submittedAt,
        LocalDateTime handledAt,
        String rejectionReason
) {}
