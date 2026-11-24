package com.mycropdiary.api.dto.farmregistration;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO phản hồi tóm tắt danh sách đơn đăng ký trang trại của người dùng.
 */
public record FarmRegistrationSummaryRespDTO(
        Integer registrationId,
        String farmName,
        String addressLine,
        String province,
        String district,
        String ward,
        String status,
        LocalDateTime submittedAt,
        LocalDateTime handledAt,
        String rejectionReason,
        BigDecimal latitude,
        BigDecimal longitude,
        String boundaryCoordinates
) {
    public FarmRegistrationSummaryRespDTO(
            Integer registrationId,
            String farmName,
            String addressLine,
            String province,
            String district,
            String ward,
            String status,
            LocalDateTime submittedAt,
            LocalDateTime handledAt,
            String rejectionReason
    ) {
        this(registrationId, farmName, addressLine, province, district, ward, status, submittedAt, handledAt, rejectionReason, null, null, null);
    }

    public FarmRegistrationSummaryRespDTO(
            Integer registrationId,
            String farmName,
            String addressLine,
            String status,
            LocalDateTime submittedAt,
            LocalDateTime handledAt,
            String rejectionReason
    ) {
        this(registrationId, farmName, addressLine, null, null, null, status, submittedAt, handledAt, rejectionReason, null, null, null);
    }
}
