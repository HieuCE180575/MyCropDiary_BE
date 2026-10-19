package com.mycropdiary.api.dto.cultivation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO tóm tắt nhật ký sử dụng vật tư.
 */
public record MaterialUsageSummaryResponse(
        UUID materialUsageId,
        UUID farmingActivityId,
        UUID materialId,
        String materialName,
        String materialType,
        LocalDateTime usedAt,
        BigDecimal quantity,
        String unit
) {}
