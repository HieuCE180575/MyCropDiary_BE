package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.block.BlockConRespDTO;
import com.mycropdiary.api.entity.BlockCondition;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BlockConditionMapper {

    public BlockConRespDTO toResponse(BlockCondition obs) {
        if (obs == null) {
            return null;
        }
        Integer recorderId = obs.getRecordedBy() != null ? obs.getRecordedBy().getId() : null;
        String recorderName = (obs.getRecordedBy() != null && obs.getRecordedBy().getUser() != null)
                ? obs.getRecordedBy().getUser().getFullName() : null;

        return new BlockConRespDTO(
                obs.getId(),
                obs.getBlock().getId(),
                obs.getBlock().getBlockName(),
                recorderId,
                recorderName,
                obs.getConditionType(),
                obs.getDescription(),
                obs.getImageUrl(),
                obs.getObservedAt()
        );
    }

    public List<BlockConRespDTO> toResponseList(List<BlockCondition> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).toList();
    }
}
