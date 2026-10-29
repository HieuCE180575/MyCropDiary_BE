package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.cultivation.CreateCropSeasonReq;
import com.mycropdiary.api.dto.cultivation.CropCategoryResp;
import com.mycropdiary.api.dto.cultivation.CropSeasonResp;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.CropCategory;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi giữa Entity CropSeason, CropCategory và các DTOs tương ứng.
 */
@Component
public class CropSeasonMapper {

    public CropSeason toEntity(CreateCropSeasonReq request, Farm farm, CropCategory category, User creator, String initialStatus) {
        if (request == null) {
            return null;
        }
        CropSeason season = new CropSeason();
        season.setFarm(farm);
        season.setCropCategory(category);
        season.setSeasonName(request.seasonName() != null ? request.seasonName().trim() : null);
        season.setStartDate(request.startDate());
        season.setEndDate(request.endDate());
        season.setSeasonStatus(initialStatus);
        season.setNotes(request.notes() != null ? request.notes().trim() : null);
        season.setCreatedBy(creator);
        return season;
    }

    public CropSeasonResp toResponse(CropSeason cs) {
        if (cs == null) {
            return null;
        }
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

    public CropCategoryResp toCategoryResponse(CropCategory c) {
        if (c == null) {
            return null;
        }
        return new CropCategoryResp(c.getId(), c.getCategoryName(), c.getDescription(), c.getImageUrl());
    }
}

