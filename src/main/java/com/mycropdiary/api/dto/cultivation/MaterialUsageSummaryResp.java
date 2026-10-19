package com.mycropdiary.api.dto.cultivation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO tóm tắt nhật ký sử dụng vật tư.
 */
public record MaterialUsageSummaryResp(
        Integer materialUsageId,
        Integer farmingActivityId,
        Integer materialId,
        String materialName,
        String materialType,
        LocalDateTime usedAt,
        BigDecimal quantity,
        String unit
) {}
