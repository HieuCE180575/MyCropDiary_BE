package com.mycropdiary.api.dto.cultivation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO tóm tắt nhật ký sử dụng vật tư.
 */
public record MaterialUsageSummaryResponse(
        Long materialUsageId,
        Long farmingActivityId,
        Long materialId,
        String materialName,
        String materialType,
        LocalDateTime usedAt,
        BigDecimal quantity,
        String unit
) {}
