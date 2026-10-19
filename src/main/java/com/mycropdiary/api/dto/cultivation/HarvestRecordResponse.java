package com.mycropdiary.api.dto.cultivation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO phản hồi chi tiết thông tin bản ghi thu hoạch (UC-26).
 */
public record HarvestRecordResponse(
        UUID harvestRecordId,
        UUID cropSeasonId,
        String seasonCode,
        String seasonName,
        UUID farmId,
        String farmName,
        UUID blockId,
        String blockName,
        UUID recordedByMemberId,
        String recorderFullName,
        String harvestLotCode,
        LocalDateTime harvestedAt,
        BigDecimal quantity,
        String unit,
        String qualityGrade,
        String storageLocation,
        String traceabilityCode,
        String notes
) {
    public HarvestRecordResponse(
            UUID harvestRecordId, UUID cropSeasonId, String seasonCode, String seasonName,
            UUID farmId, String farmName, UUID plotId, String plotCode, String plotName,
            UUID recordedByMemberId, String recorderFullName, String harvestLotCode,
            LocalDateTime harvestedAt, BigDecimal quantity, String unit, String qualityGrade,
            String storageLocation, String traceabilityCode, String notes
    ) {
        this(harvestRecordId, cropSeasonId, seasonCode, seasonName, farmId, farmName, plotId, plotName != null ? plotName : plotCode, recordedByMemberId, recorderFullName, harvestLotCode, harvestedAt, quantity, unit, qualityGrade, storageLocation, traceabilityCode, notes);
    }

    public UUID plotId() { return blockId; }
    public String plotCode() { return blockName; }
    public String plotName() { return blockName; }
}
