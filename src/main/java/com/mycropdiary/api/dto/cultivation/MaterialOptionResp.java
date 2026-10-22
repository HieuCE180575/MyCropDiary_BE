package com.mycropdiary.api.dto.cultivation;

import com.mycropdiary.api.entity.cultivation.Material;

/**
 * DTO đại diện cho danh mục vật tư nông nghiệp của nông trại.
 */
public record MaterialOptionResp(
        Integer id,
        Integer farmId,
        String materialName,
        String materialType,
        String unit,
        String description,
        Boolean isActive
) {
    public static MaterialOptionResp fromEntity(Material material) {
        return new MaterialOptionResp(
                material.getId(),
                material.getFarm() != null ? material.getFarm().getId() : null,
                material.getMaterialName(),
                material.getMaterialType(),
                material.getUnit(),
                material.getDescription(),
                material.getIsActive()
        );
    }
}
