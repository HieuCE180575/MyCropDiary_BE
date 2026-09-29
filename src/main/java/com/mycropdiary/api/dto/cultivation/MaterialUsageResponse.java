package com.mycropdiary.api.dto.cultivation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin chi tiết nhật ký sử dụng vật tư (UC-25).
 */
public record MaterialUsageResponse(
        Long materialUsageId,
        Long farmingActivityId,
        String activityName,
        Long cropSeasonId,
        Long materialId,
        String materialCode,
        String materialName,
        String materialType,
        Long inputPurchaseDetailId,
        Long recordedByMemberId,
        String recorderFullName,
        LocalDateTime usedAt,
        BigDecimal quantity,
        String unit,
        String dosage,
        String method,
        Integer safetyIntervalDays,
        String notes
) {}
