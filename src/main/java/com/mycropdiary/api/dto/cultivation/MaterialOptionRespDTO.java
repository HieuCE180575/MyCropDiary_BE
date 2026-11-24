package com.mycropdiary.api.dto.cultivation;

import com.mycropdiary.api.entity.cultivation.Material;

/**
 * DTO đại diện cho danh mục vật tư nông nghiệp của nông trại.
 */
public record MaterialOptionRespDTO(
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
    public MaterialOptionRespDTO(
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

    public MaterialOptionRespDTO(
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

    public static MaterialOptionRespDTO fromEntity(Material material) {
        return new MaterialOptionRespDTO(
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
