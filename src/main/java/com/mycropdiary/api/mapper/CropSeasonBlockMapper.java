package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.cultivation.AssignCropSeasonBlockReq;
import com.mycropdiary.api.dto.cultivation.CropSeasonBlockResp;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.CropSeasonBlock;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Mapper chuyển đổi giữa Entity CropSeasonBlock và DTOs tương ứng.
 */
@Component
public class CropSeasonBlockMapper {

    public CropSeasonBlock toEntity(CropSeason season, Block block, BigDecimal plantedArea, String areaUnit, LocalDate plantedAt, LocalDate expectedHarvestAt) {
        return new CropSeasonBlock(season, block, plantedArea, areaUnit, plantedAt, expectedHarvestAt);
    }

    public CropSeasonBlockResp toResponse(CropSeasonBlock csb) {
        if (csb == null) {
            return null;
        }
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

