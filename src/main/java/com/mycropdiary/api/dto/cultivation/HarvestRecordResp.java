package com.mycropdiary.api.dto.cultivation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO phản hồi chi tiết thông tin bản ghi thu hoạch (UC-26).
 */
public record HarvestRecordResp(
        Integer harvestRecordId,
        Integer cropSeasonId,
        String seasonCode,
        String seasonName,
        Integer farmId,
        String farmName,
        Integer blockId,
        String blockName,
        Integer recordedByMemberId,
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
    public HarvestRecordResp(
            Integer harvestRecordId, Integer cropSeasonId, String seasonCode, String seasonName,
            Integer farmId, String farmName, Integer plotId, String plotCode, String plotName,
            Integer recordedByMemberId, String recorderFullName, String harvestLotCode,
            LocalDateTime harvestedAt, BigDecimal quantity, String unit, String qualityGrade,
            String storageLocation, String traceabilityCode, String notes
    ) {
        this(harvestRecordId, cropSeasonId, seasonCode, seasonName, farmId, farmName, plotId, plotName != null ? plotName : plotCode, recordedByMemberId, recorderFullName, harvestLotCode, harvestedAt, quantity, unit, qualityGrade, storageLocation, traceabilityCode, notes);
    }

    public Integer plotId() { return blockId; }
    public String plotCode() { return blockName; }
    public String plotName() { return blockName; }
}
