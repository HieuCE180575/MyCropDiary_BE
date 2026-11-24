package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.cultivation.HarvestRecordRespDTO;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.HarvestRecord;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Lớp chuyển đổi dữ liệu (Mapper) giữa Entity HarvestRecord và HarvestRecordResp DTO.
 */
@Component
public class HarvestRecordMapper {

    public HarvestRecordRespDTO toResponse(HarvestRecord record) {
        CropSeason season = record.getCropSeason();
        Farm farm = season != null ? season.getFarm() : record.getFarm();
        Block block = record.getBlock();

        String recorderName = record.getRecordedBy() != null && record.getRecordedBy().getUser() != null
                ? record.getRecordedBy().getUser().getFullName()
                : null;

        return new HarvestRecordRespDTO(
                record.getId(),
                season != null ? season.getId() : null,
                season != null ? season.getSeasonName() : null,
                season != null ? season.getSeasonName() : null,
                farm != null ? farm.getId() : null,
                farm != null ? farm.getFarmName() : null,
                block != null ? block.getId() : null,
                block != null ? block.getBlockName() : null,
                block != null ? block.getBlockName() : null,
                record.getRecordedBy() != null ? record.getRecordedBy().getId() : null,
                recorderName,
                record.getHarvestLotCode(),
                record.getHarvestedAt(),
                record.getQuantity(),
                record.getDiscardedQuantity() != null ? record.getDiscardedQuantity() : BigDecimal.ZERO,
                record.getUnit(),
                record.getQualityGrade(),
                record.getStorageLocation(),
                record.getTraceabilityCode(),
                record.getNotes()
        );
    }
}
