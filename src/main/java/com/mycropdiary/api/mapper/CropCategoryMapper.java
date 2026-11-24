package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.cultivation.CropCategoryRespDTO;
import com.mycropdiary.api.entity.cultivation.CropCategory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CropCategoryMapper {

    public CropCategoryRespDTO toResponse(CropCategory c) {
        if (c == null) {
            return null;
        }
        return new CropCategoryRespDTO(c.getId(), c.getCategoryName(), c.getDescription(), c.getImageUrl(), c.getStatus());
    }

    public List<CropCategoryRespDTO> toResponseList(List<CropCategory> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).toList();
    }
}
