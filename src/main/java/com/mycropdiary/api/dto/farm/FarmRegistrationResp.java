package com.mycropdiary.api.dto.farm;

import java.time.LocalDateTime;

public record FarmRegistrationResp(
        Integer registrationId,
        Integer applicantUserId,
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
        Integer handlerUserId,
        String handlerFullName,
        LocalDateTime submittedAt,
        LocalDateTime handledAt,
        String rejectionReason
) {}
