package com.mycropdiary.api.dto.assignment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO phản hồi vùng được phân công cho nhân viên (Assigned Zone).
 */
public record AssignedZoneResponse(
        UUID zoneId,
        UUID farmId,
        String zoneName,
        BigDecimal areaValue,
        String areaUnit,
        String description,
        String status,
        UUID assignmentId,
        Instant assignedStartDate,
        int activeBlockCount
) {
    public UUID productionAreaId() { return zoneId; }
    public String areaName() { return zoneName; }
    public String areaCode() { return zoneName; }
    public BigDecimal areaM2() { return areaValue; }
    public int activePlotCount() { return activeBlockCount; }
}
