package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.compliance.EquipmentMaintenanceRespDTO;
import com.mycropdiary.api.dto.compliance.FarmEquipmentRespDTO;
import com.mycropdiary.api.entity.compliance.EquipmentMaintenanceLog;
import com.mycropdiary.api.entity.compliance.FarmEquipment;
import org.springframework.stereotype.Component;

@Component
public class FarmEquipmentMapper {

    public FarmEquipmentRespDTO toEquipmentResponse(FarmEquipment e) {
        return new FarmEquipmentRespDTO(
                e.getId(),
                e.getFarm() != null ? e.getFarm().getId() : null,
                e.getEquipmentCode(),
                e.getEquipmentName(),
                e.getEquipmentType(),
                e.getStatus(),
                e.getNotes(),
                e.getCreatedAt()
        );
    }

    public EquipmentMaintenanceRespDTO toMaintenanceResponse(EquipmentMaintenanceLog log) {
        return new EquipmentMaintenanceRespDTO(
                log.getId(),
                log.getFarm() != null ? log.getFarm().getId() : null,
                log.getEquipment() != null ? log.getEquipment().getId() : null,
                log.getEquipmentCode(),
                log.getEquipmentName(),
                log.getActionType(),
                log.getActionDate(),
                log.getPerformerName(),
                log.getRinseWaterHandling(),
                log.getIsSafeForUse(),
                log.getNotes(),
                log.getCreatedAt()
        );
    }
}
