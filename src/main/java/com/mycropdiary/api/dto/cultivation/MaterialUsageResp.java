package com.mycropdiary.api.dto.cultivation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO phản hồi thông tin chi tiết nhật ký sử dụng vật tư (UC-25).
 */
public record MaterialUsageResp(
        Integer materialUsageId,
        Integer farmingActivityId,
        String activityName,
        Integer cropSeasonId,
        Integer materialId,
        String materialCode,
        String materialName,
        String materialType,
        Integer inputPurchaseDetailId,
        Integer recordedByMemberId,
        String recorderFullName,
        LocalDateTime usedAt,
        BigDecimal quantity,
        String unit,
        String dosage,
        String method,
        Integer safetyIntervalDays,
        String notes
) {}
