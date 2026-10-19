package com.mycropdiary.api.dto.farm;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * DTO phản hồi thông tin phân công nhân viên quản lý Vùng canh tác / Zone.
 */
public record StaffZoneAssignmentResp(
        Integer id,
        Integer farmMemberId,
        Integer zoneId,
        String zoneName,
        Integer assignedByMemberId,
        Instant assignedAt,
        Instant endedAt,
        boolean active
) {
    public StaffZoneAssignmentResp(Integer id, Integer farmMemberId, Integer zoneId, String zoneName, Integer assignedByMemberId, LocalDate startDate, LocalDate endDate, boolean active) {
        this(id, farmMemberId, zoneId, zoneName, assignedByMemberId,
                startDate != null ? startDate.atStartOfDay(ZoneOffset.UTC).toInstant() : null,
                endDate != null ? endDate.atStartOfDay(ZoneOffset.UTC).toInstant() : null,
                active);
    }

    public Integer productionAreaId() { return zoneId; }
    public String areaName() { return zoneName; }
    public String areaCode() { return zoneName; }
    public Instant startDate() { return assignedAt; }
    public Instant endDate() { return endedAt; }
}
