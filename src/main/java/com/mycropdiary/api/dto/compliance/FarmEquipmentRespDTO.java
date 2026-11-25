package com.mycropdiary.api.dto.compliance;

import java.time.Instant;

public record FarmEquipmentRespDTO(
        Integer equipmentId,
        Integer farmId,
        String equipmentCode,
        String equipmentName,
        String equipmentType,
        String status,
        String notes,
        Instant createdAt
) {}
