package com.mycropdiary.api.dto.assignment;

import java.time.Instant;

/**
 * DTO phản hồi thông tin chi tiết phân công nhân viên vào Vùng / Zone.
 */
public record AssignmentResp(
        Integer id,
        Integer farmMemberId,
        Integer userId,
        String staffName,
        String staffEmail,
        String staffJobTitle,
        Integer zoneId,
        String zoneName,
        Integer assignedByMemberId,
        String assignerName,
        Instant startDate,
        Instant endDate,
        Boolean isActive
) {
    public AssignmentResp(Integer id, Integer farmMemberId, Integer userId, String staffName, String staffEmail, String staffJobTitle, Integer productionAreaId, String areaCode, String areaName, Integer assignedByMemberId, String assignerName, Instant startDate, Instant endDate, Boolean isActive) {
        this(id, farmMemberId, userId, staffName, staffEmail, staffJobTitle, productionAreaId, areaName != null ? areaName : areaCode, assignedByMemberId, assignerName, startDate, endDate, isActive);
    }

    public Integer productionAreaId() { return zoneId; }
    public String areaName() { return zoneName; }
    public String areaCode() { return zoneName; }
}
