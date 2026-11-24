package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.cultivation.LotTraceabilityRespDTO;
import com.mycropdiary.api.entity.cultivation.HarvestRecord;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TraceabilityMapper {

    public LotTraceabilityRespDTO toResponse(HarvestRecord record) {
        if (record == null) {
            return null;
        }
        return new LotTraceabilityRespDTO(
                record.getId(),
                record.getHarvestLotCode(),
                record.getTraceabilityCode(),
                record.getFarm() != null ? record.getFarm().getId() : null,
                record.getFarm() != null ? record.getFarm().getFarmName() : null,
                record.getCropSeason() != null ? record.getCropSeason().getId() : null,
                record.getCropSeason() != null ? record.getCropSeason().getSeasonName() : null,
                record.getBlock() != null ? record.getBlock().getId() : null,
                record.getBlock() != null ? record.getBlock().getBlockName() : null,
                record.getHarvestedAt(),
                record.getQuantity(),
                record.getUnit(),
                record.getQualityGrade(),
                record.getNotes()
        );
    }

    public List<LotTraceabilityRespDTO> toResponseList(List<HarvestRecord> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).toList();
    }
}
