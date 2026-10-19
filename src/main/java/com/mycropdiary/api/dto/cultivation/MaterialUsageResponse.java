package com.mycropdiary.api.dto.cultivation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO phản hồi thông tin chi tiết nhật ký sử dụng vật tư (UC-25).
 */
public record MaterialUsageResponse(
        UUID materialUsageId,
        UUID farmingActivityId,
        String activityName,
        UUID cropSeasonId,
        UUID materialId,
        String materialCode,
        String materialName,
        String materialType,
        UUID inputPurchaseDetailId,
        UUID recordedByMemberId,
        String recorderFullName,
        LocalDateTime usedAt,
        BigDecimal quantity,
        String unit,
        String dosage,
        String method,
        Integer safetyIntervalDays,
        String notes
) {}
