package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.assignment.AssignedAreaResponse;
import com.mycropdiary.api.dto.assignment.AssignmentResponse;
import com.mycropdiary.api.dto.assignment.CreateAssignmentRequest;

import java.util.List;

public interface AreaAssignmentService {
    AssignmentResponse assignStaffToArea(Long currentUserId, Long farmId, CreateAssignmentRequest request);
    void unassignStaffFromArea(Long currentUserId, Long farmId, Long assignmentId);
    List<AssignedAreaResponse> getAssignedAreasForCurrentStaff(Long currentUserId, Long farmId);
    List<AssignmentResponse> getAreaAssignments(Long currentUserId, Long farmId, Long productionAreaId);
}
