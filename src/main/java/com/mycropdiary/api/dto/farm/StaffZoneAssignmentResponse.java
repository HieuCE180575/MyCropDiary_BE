package com.mycropdiary.api.dto.farm;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * DTO phản hồi thông tin phân công nhân viên quản lý Vùng canh tác / Zone.
 */
public record StaffZoneAssignmentResponse(
        UUID id,
        UUID farmMemberId,
        UUID zoneId,
        String zoneName,
        UUID assignedByMemberId,
        Instant assignedAt,
        Instant endedAt,
        boolean active
) {
    public StaffZoneAssignmentResponse(UUID id, UUID farmMemberId, UUID zoneId, String zoneName, UUID assignedByMemberId, LocalDate startDate, LocalDate endDate, boolean active) {
        this(id, farmMemberId, zoneId, zoneName, assignedByMemberId,
                startDate != null ? startDate.atStartOfDay(ZoneOffset.UTC).toInstant() : null,
                endDate != null ? endDate.atStartOfDay(ZoneOffset.UTC).toInstant() : null,
                active);
    }

    public UUID productionAreaId() { return zoneId; }
    public String areaName() { return zoneName; }
    public String areaCode() { return zoneName; }
    public Instant startDate() { return assignedAt; }
    public Instant endDate() { return endedAt; }
}
