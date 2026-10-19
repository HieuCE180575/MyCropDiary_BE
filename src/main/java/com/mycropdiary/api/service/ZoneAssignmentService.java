package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.assignment.AssignedZoneResponse;
import com.mycropdiary.api.dto.assignment.AssignmentResponse;
import com.mycropdiary.api.dto.assignment.CreateAssignmentRequest;

import java.util.List;
import java.util.UUID;

public interface ZoneAssignmentService {
    AssignmentResponse assignStaffToZone(UUID currentUserId, UUID farmId, CreateAssignmentRequest request);
    void unassignStaffFromZone(UUID currentUserId, UUID farmId, UUID assignmentId);
    List<AssignedZoneResponse> getAssignedZonesForCurrentStaff(UUID currentUserId, UUID farmId);
    List<AssignmentResponse> getZoneAssignments(UUID currentUserId, UUID farmId, UUID zoneId);
}
