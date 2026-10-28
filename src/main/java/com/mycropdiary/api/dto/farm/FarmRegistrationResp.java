package com.mycropdiary.api.dto.farm;

import java.math.BigDecimal;
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
        String rejectionReason,
        BigDecimal latitude,
        BigDecimal longitude,
        String boundaryCoordinates
) {
    public FarmRegistrationResp(
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
    ) {
        this(registrationId, applicantUserId, applicantEmail, applicantFullName, farmName, addressLine, province, district, ward, description, documentUrl, status, handlerUserId, handlerFullName, submittedAt, handledAt, rejectionReason, null, null, null);
    }
}
