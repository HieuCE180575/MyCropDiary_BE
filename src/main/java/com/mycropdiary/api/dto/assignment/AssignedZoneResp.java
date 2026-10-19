package com.mycropdiary.api.dto.assignment;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO phản hồi vùng được phân công cho nhân viên (Assigned Zone).
 */
public record AssignedZoneResp(
        Integer zoneId,
        Integer farmId,
        String zoneName,
        BigDecimal areaValue,
        String areaUnit,
        String description,
        String status,
        Integer assignmentId,
        Instant assignedStartDate,
        int activeBlockCount
) {
    public Integer productionAreaId() { return zoneId; }
    public String areaName() { return zoneName; }
    public String areaCode() { return zoneName; }
    public BigDecimal areaM2() { return areaValue; }
    public int activePlotCount() { return activeBlockCount; }
}
