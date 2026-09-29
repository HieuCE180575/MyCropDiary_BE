package com.mycropdiary.api.dto.farmregistration;

import com.mycropdiary.api.entity.farmregistration.FarmRegistrationStatus;
import java.time.LocalDateTime;

/**
 * DTO phản hồi tóm tắt danh sách đơn đăng ký trang trại của người dùng.
 */
public record FarmRegistrationSummaryResponse(
        Long registrationId,
        String farmName,
        String addressLine,
        FarmRegistrationStatus status,
        LocalDateTime submittedAt,
        LocalDateTime handledAt,
        String rejectionReason
) {}
