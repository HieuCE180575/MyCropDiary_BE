package com.mycropdiary.api.dto.compliance;

import java.time.Instant;
import java.time.LocalDate;

public record EquipmentMaintenanceRespDTO(
        Integer maintenanceId,
        Integer farmId,
        Integer equipmentId,
        String equipmentCode,
        String equipmentName,
        String actionType,
        LocalDate actionDate,
        String performerName,
        String rinseWaterHandling,
        Boolean isSafeForUse,
        String notes,
        Instant createdAt
) {}
