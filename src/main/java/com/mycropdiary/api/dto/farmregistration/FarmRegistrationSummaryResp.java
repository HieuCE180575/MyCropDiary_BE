package com.mycropdiary.api.dto.farmregistration;

import com.mycropdiary.api.entity.farmregistration.FarmRegistrationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO phản hồi tóm tắt danh sách đơn đăng ký trang trại của người dùng.
 */
public record FarmRegistrationSummaryResp(
        Integer registrationId,
        String farmName,
        String addressLine,
        String province,
        String district,
        String ward,
        FarmRegistrationStatus status,
        LocalDateTime submittedAt,
        LocalDateTime handledAt,
        String rejectionReason,
        BigDecimal latitude,
        BigDecimal longitude,
        String boundaryCoordinates
) {
    public FarmRegistrationSummaryResp(
            Integer registrationId,
            String farmName,
            String addressLine,
            String province,
            String district,
            String ward,
            FarmRegistrationStatus status,
            LocalDateTime submittedAt,
            LocalDateTime handledAt,
            String rejectionReason
    ) {
        this(registrationId, farmName, addressLine, province, district, ward, status, submittedAt, handledAt, rejectionReason, null, null, null);
    }

    public FarmRegistrationSummaryResp(
            Integer registrationId,
            String farmName,
            String addressLine,
            FarmRegistrationStatus status,
            LocalDateTime submittedAt,
            LocalDateTime handledAt,
            String rejectionReason
    ) {
        this(registrationId, farmName, addressLine, null, null, null, status, submittedAt, handledAt, rejectionReason, null, null, null);
    }
}
