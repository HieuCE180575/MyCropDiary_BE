package com.mycropdiary.api.dto.assignment;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO phản hồi thông tin chi tiết phân công nhân viên vào Vùng / Zone.
 */
public record AssignmentResponse(
        UUID id,
        UUID farmMemberId,
        UUID userId,
        String staffName,
        String staffEmail,
        String staffJobTitle,
        UUID zoneId,
        String zoneName,
        UUID assignedByMemberId,
        String assignerName,
        Instant startDate,
        Instant endDate,
        Boolean isActive
) {
    public AssignmentResponse(UUID id, UUID farmMemberId, UUID userId, String staffName, String staffEmail, String staffJobTitle, UUID productionAreaId, String areaCode, String areaName, UUID assignedByMemberId, String assignerName, Instant startDate, Instant endDate, Boolean isActive) {
        this(id, farmMemberId, userId, staffName, staffEmail, staffJobTitle, productionAreaId, areaName != null ? areaName : areaCode, assignedByMemberId, assignerName, startDate, endDate, isActive);
    }

    public UUID productionAreaId() { return zoneId; }
    public String areaName() { return zoneName; }
    public String areaCode() { return zoneName; }
}
