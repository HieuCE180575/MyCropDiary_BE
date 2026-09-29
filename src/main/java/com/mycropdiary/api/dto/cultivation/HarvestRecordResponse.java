package com.mycropdiary.api.dto.cultivation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO phản hồi chi tiết thông tin bản ghi thu hoạch (UC-26).
 */
public record HarvestRecordResponse(
        Long harvestRecordId,
        Long cropSeasonId,
        String seasonCode,
        String seasonName,
        Long farmId,
        String farmName,
        Long plotId,
        String plotCode,
        String plotName,
        Long recordedByMemberId,
        String recorderFullName,
        String harvestLotCode,
        LocalDateTime harvestedAt,
        BigDecimal quantity,
        String unit,
        String qualityGrade,
        String storageLocation,
        String traceabilityCode,
        String notes
) {}
