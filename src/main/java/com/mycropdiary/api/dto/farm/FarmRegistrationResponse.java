package com.mycropdiary.api.dto.farm;

import java.time.LocalDateTime;
import java.util.UUID;

public record FarmRegistrationResponse(
        UUID registrationId,
        UUID applicantUserId,
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
        UUID handlerUserId,
        String handlerFullName,
        LocalDateTime submittedAt,
        LocalDateTime handledAt,
        String rejectionReason
) {}
