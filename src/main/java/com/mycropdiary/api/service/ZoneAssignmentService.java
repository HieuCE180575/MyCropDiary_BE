package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.assignment.AssignedZoneResp;
import com.mycropdiary.api.dto.assignment.AssignmentResp;
import com.mycropdiary.api.dto.assignment.CreateAssignmentReq;

import java.util.List;

public interface ZoneAssignmentService {
    AssignmentResp assignStaffToZone(Integer currentUserId, Integer farmId, CreateAssignmentReq request);
    void unassignStaffFromZone(Integer currentUserId, Integer farmId, Integer assignmentId);
    List<AssignedZoneResp> getAssignedZonesForCurrentStaff(Integer currentUserId, Integer farmId);
    List<AssignmentResp> getZoneAssignments(Integer currentUserId, Integer farmId, Integer zoneId);
}
