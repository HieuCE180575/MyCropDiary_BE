package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.assignment.AssignedZoneRespDTO;
import com.mycropdiary.api.dto.assignment.AssignmentRespDTO;
import com.mycropdiary.api.dto.assignment.CreateAssignmentReqDTO;

import java.util.List;

public interface ZoneAssignmentService {
    AssignmentRespDTO assignStaffToZone(Integer currentUserId, Integer farmId, CreateAssignmentReqDTO request);
    void unassignStaffFromZone(Integer currentUserId, Integer farmId, Integer assignmentId);
    List<AssignedZoneRespDTO> getAssignedZonesForCurrentStaff(Integer currentUserId, Integer farmId);
    List<AssignmentRespDTO> getZoneAssignments(Integer currentUserId, Integer farmId, Integer zoneId);
}
