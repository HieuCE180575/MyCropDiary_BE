package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.cultivation.MaterialUsageRespDTO;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.FarmingActivity;
import com.mycropdiary.api.entity.cultivation.Material;
import com.mycropdiary.api.entity.cultivation.MaterialUsage;
import org.springframework.stereotype.Component;

/**
 * Lớp chuyển đổi dữ liệu (Mapper) giữa Entity MaterialUsage và MaterialUsageResp DTO.
 */
@Component
public class MaterialUsageMapper {

    public MaterialUsageRespDTO toResponse(MaterialUsage usage, String resolvedMaterialForm) {
        String recorderName = usage.getUsedBy() != null && usage.getUsedBy().getUser() != null
                ? usage.getUsedBy().getUser().getFullName()
                : null;
        FarmingActivity act = usage.getFarmingActivity();
        CropSeason cs = act != null ? act.getCropSeason() : null;
        Material mat = usage.getMaterial();

        return new MaterialUsageRespDTO(
                usage.getId(),
                act != null ? act.getId() : null,
                act != null ? act.getActivityName() : null,
                cs != null ? cs.getId() : null,
                mat != null ? mat.getId() : null,
                mat != null ? mat.getMaterialName() : null,
                mat != null ? mat.getMaterialName() : null,
                mat != null ? mat.getMaterialType() : null,
                resolvedMaterialForm,
                null,
                usage.getUsedBy() != null ? usage.getUsedBy().getId() : null,
                recorderName,
                usage.getUsedAtLdt(),
                usage.getQuantity(),
                usage.getUnit(),
                usage.getDosage(),
                usage.getMethod(),
                usage.getAppliedArea(),
                usage.getAppliedAreaUnit(),
                usage.getAppliedPlantCount(),
                usage.getSafetyIntervalDays(),
                usage.getNotes()
        );
    }
}
