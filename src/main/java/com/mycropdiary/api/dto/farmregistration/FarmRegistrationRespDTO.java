package com.mycropdiary.api.dto.farmregistration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO phản hồi chi tiết thông tin đơn đăng ký trang trại.
 */
public record FarmRegistrationRespDTO(
        Integer registrationId,
        String farmName,
        String addressLine,
        String province,
        String district,
        String ward,
        String description,
        String documentUrl,
        String status,
        LocalDateTime submittedAt,
        LocalDateTime handledAt,
        String rejectionReason,
        BigDecimal latitude,
        BigDecimal longitude,
        String boundaryCoordinates
) {
    public FarmRegistrationRespDTO(
            Integer registrationId,
            String farmName,
            String addressLine,
            String province,
            String district,
            String ward,
            String description,
            String documentUrl,
            String status,
            LocalDateTime submittedAt,
            LocalDateTime handledAt,
            String rejectionReason
    ) {
        this(registrationId, farmName, addressLine, province, district, ward, description, documentUrl, status, submittedAt, handledAt, rejectionReason, null, null, null);
    }
}
