package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.cultivation.MaterialUsageResp;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.FarmingActivity;
import com.mycropdiary.api.entity.cultivation.MaterialUsage;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi giữa Entity MaterialUsage và DTOs tương ứng.
 */
@Component
public class MaterialUsageMapper {

    public MaterialUsageResp toResponse(MaterialUsage usage) {
        if (usage == null) {
            return null;
        }
        String recorderName = usage.getUsedBy() != null && usage.getUsedBy().getUser() != null
                ? usage.getUsedBy().getUser().getFullName()
                : null;
        FarmingActivity act = usage.getFarmingActivity();
        CropSeason cs = act != null ? act.getCropSeason() : null;

        return new MaterialUsageResp(
                usage.getId(),
                act != null ? act.getId() : null,
                act != null ? act.getActivityName() : null,
                cs != null ? cs.getId() : null,
                usage.getMaterial() != null ? usage.getMaterial().getId() : null,
                usage.getMaterial() != null ? usage.getMaterial().getMaterialName() : null,
                usage.getMaterial() != null ? usage.getMaterial().getMaterialName() : null,
                usage.getMaterial() != null ? usage.getMaterial().getMaterialType() : null,
                null,
                usage.getUsedBy() != null ? usage.getUsedBy().getId() : null,
                recorderName,
                usage.getUsedAtLdt(),
                usage.getQuantity(),
                usage.getUnit(),
                usage.getDosage(),
                usage.getMethod(),
                usage.getSafetyIntervalDays(),
                usage.getNotes()
        );
    }
}
