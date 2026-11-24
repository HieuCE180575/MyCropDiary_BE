package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.block.BlockConRespDTO;
import com.mycropdiary.api.dto.block.BlockRespDTO;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.BlockCondition;
import org.springframework.stereotype.Component;

/**
 * Lớp chuyển đổi dữ liệu (Mapper) giữa các JPA Entities và DTOs của Module Block.
 */
@Component
public class BlockMapper {

    public BlockRespDTO toBlockResponse(Block b) {
        return new BlockRespDTO(
                b.getId(),
                b.getZone().getId(),
                b.getZone().getZoneName(),
                b.getZone().getFarm().getId(),
                b.getBlockName(),
                b.getAreaValue(),
                b.getAreaUnit(),
                b.getSoilType(),
                b.getDescription(),
                b.getImageUrl(),
                "AVAILABLE",
                null,
                null,
                null,
                b.getCreatedAt()
        );
    }

    public BlockConRespDTO toBlockConditionResponse(BlockCondition obs) {
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
}
