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
        String materialForm,
        Integer inputPurchaseDetailId,
        Integer recordedByMemberId,
        String recorderFullName,
        LocalDateTime usedAt,
        BigDecimal quantity,
        String unit,
        String dosage,
        String method,
        BigDecimal appliedArea,
        String appliedAreaUnit,
        Integer appliedPlantCount,
        Integer safetyIntervalDays,
        String notes
) {
    public MaterialUsageResp(
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
    ) {
        this(
                materialUsageId,
                farmingActivityId,
                activityName,
                cropSeasonId,
                materialId,
                materialCode,
                materialName,
                materialType,
                null,
                inputPurchaseDetailId,
                recordedByMemberId,
                recorderFullName,
                usedAt,
                quantity,
                unit,
                dosage,
                method,
                null,
                null,
                null,
                safetyIntervalDays,
                notes
        );
    }
}
