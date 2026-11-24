package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.cultivation.CropSeasonBlockResp;
import com.mycropdiary.api.dto.cultivation.CropSeasonResp;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.CropSeasonBlock;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Lớp chuyển đổi dữ liệu (Mapper) giữa các JPA Entities và DTOs của Module Crop Season.
 */
@Component
public class CropSeasonMapper {

    public CropSeasonResp toResponse(CropSeason cs) {
        String farmName = cs.getFarm() != null ? cs.getFarm().getFarmName() : null;
        Integer cropCatId = cs.getCropCategory() != null ? cs.getCropCategory().getId() : null;
        String cropCatName = cs.getCropCategory() != null ? cs.getCropCategory().getCategoryName() : null;
        Integer creatorId = cs.getCreatedBy() != null ? cs.getCreatedBy().getId() : null;
        String creatorName = cs.getCreatedBy() != null ? cs.getCreatedBy().getFullName() : null;

        return new CropSeasonResp(
                cs.getId(),
                cs.getFarm() != null ? cs.getFarm().getId() : null,
                farmName,
                cropCatId,
                cropCatName,
                cs.getSeasonName(),
                cs.getStartDate(),
                cs.getEndDate(),
                cs.getSeasonStatus(),
                cs.getNotes(),
                creatorId,
                creatorName,
                cs.getCreatedAt()
        );
    }

    public CropSeasonBlockResp toBlockResponse(CropSeasonBlock csb) {
        CropSeason season = csb.getCropSeason();
        Block block = csb.getBlock();

        Integer seasonId = season != null ? season.getId() : null;
        String seasonName = season != null ? season.getSeasonName() : null;
        Integer blockId = block != null ? block.getId() : null;
        String blockName = block != null ? block.getBlockName() : null;
        Integer zoneId = (block != null && block.getZone() != null) ? block.getZone().getId() : null;
        String zoneName = (block != null && block.getZone() != null) ? block.getZone().getZoneName() : null;
        BigDecimal blockArea = block != null ? block.getAreaValue() : null;
        String blockAreaUnit = block != null ? block.getAreaUnit() : null;
        String soilType = block != null ? block.getSoilType() : null;

        return new CropSeasonBlockResp(
                seasonId,
                seasonName,
                blockId,
                blockName,
                zoneId,
                zoneName,
                blockArea,
                blockAreaUnit,
                soilType,
                csb.getPlantedArea(),
                csb.getAreaUnit(),
                csb.getPlantedAt(),
                csb.getExpectedHarvestAt()
        );
    }
}
