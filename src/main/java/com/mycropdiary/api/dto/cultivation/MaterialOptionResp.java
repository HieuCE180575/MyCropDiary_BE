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
        String materialForm,
        String unit,
        String description,
        String lotNumber,
        java.math.BigDecimal germinationRate,
        Boolean isActive
) {
    public MaterialOptionResp(
            Integer id,
            Integer farmId,
            String materialName,
            String materialType,
            String materialForm,
            String unit,
            String description,
            Boolean isActive
    ) {
        this(id, farmId, materialName, materialType, materialForm, unit, description, null, null, isActive);
    }

    public MaterialOptionResp(
            Integer id,
            Integer farmId,
            String materialName,
            String materialType,
            String unit,
            String description,
            Boolean isActive
    ) {
        this(id, farmId, materialName, materialType, null, unit, description, null, null, isActive);
    }

    public static MaterialOptionResp fromEntity(Material material) {
        return new MaterialOptionResp(
                material.getId(),
                material.getFarm() != null ? material.getFarm().getId() : null,
                material.getMaterialName(),
                material.getMaterialType(),
                material.getMaterialForm(),
                material.getUnit(),
                material.getDescription(),
                material.getLotNumber(),
                material.getGerminationRate(),
                material.getIsActive()
        );
    }
}
