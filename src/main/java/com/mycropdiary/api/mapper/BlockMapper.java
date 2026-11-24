package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.block.BlockConditionResp;
import com.mycropdiary.api.dto.block.BlockResp;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.BlockConditionObservation;
import org.springframework.stereotype.Component;

/**
 * Lớp chuyển đổi dữ liệu (Mapper) giữa các JPA Entities và DTOs của Module Block.
 */
@Component
public class BlockMapper {

    public BlockResp toBlockResponse(Block b) {
        return new BlockResp(
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

    public BlockConditionResp toBlockConditionResponse(BlockConditionObservation obs) {
        Integer recorderId = obs.getRecordedBy() != null ? obs.getRecordedBy().getId() : null;
        String recorderName = (obs.getRecordedBy() != null && obs.getRecordedBy().getUser() != null)
                ? obs.getRecordedBy().getUser().getFullName() : null;

        return new BlockConditionResp(
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
