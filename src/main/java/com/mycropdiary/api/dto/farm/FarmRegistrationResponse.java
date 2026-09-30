package com.mycropdiary.api.dto.farm;

import java.time.LocalDateTime;

// [AI_CHANGE] Root cause: UC-39 cần DTO trả về thông tin đơn đăng ký farm cho Admin xem và xử lý
// [AI_CHANGE] Mechanism: Java Record chứa đầy đủ thông tin FarmRegistration + applicant info
public record FarmRegistrationResponse(
        Long registrationId,
        Long applicantUserId,
        String applicantEmail,
        String applicantFullName,
        String farmName,
        String addressLine,
        String province,
        String district,
        String ward,
        String description,
        String documentUrl,
        String status,
        Long handlerUserId,
        String handlerFullName,
        LocalDateTime submittedAt,
        LocalDateTime handledAt,
        String rejectionReason
) {}
