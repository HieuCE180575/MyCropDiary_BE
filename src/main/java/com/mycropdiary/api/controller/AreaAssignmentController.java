package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.assignment.AssignedAreaResponse;
import com.mycropdiary.api.dto.assignment.AssignmentResponse;
import com.mycropdiary.api.dto.assignment.CreateAssignmentRequest;
import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.util.SecurityUtils;
import com.mycropdiary.api.service.AreaAssignmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/farms/{farmId}")
public class AreaAssignmentController {
    private final AreaAssignmentService areaAssignmentService;
    private final SecurityUtils securityUtils;

    public AreaAssignmentController(AreaAssignmentService areaAssignmentService, SecurityUtils securityUtils) {
        this.areaAssignmentService = areaAssignmentService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/area-assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AssignmentResponse> assignStaffToArea(
            @PathVariable Long farmId,
            @Valid @RequestBody CreateAssignmentRequest request) {
        Long currentUserId = securityUtils.getRequiredCurrentUserId();
        AssignmentResponse response = areaAssignmentService.assignStaffToArea(currentUserId, farmId, request);
        return ApiResponse.ok("Phân công nhân viên vào khu vực thành công", response);
    }

    @DeleteMapping("/area-assignments/{assignmentId}")
    public ApiResponse<Void> unassignStaffFromArea(
            @PathVariable Long farmId,
            @PathVariable Long assignmentId) {
        Long currentUserId = securityUtils.getRequiredCurrentUserId();
        areaAssignmentService.unassignStaffFromArea(currentUserId, farmId, assignmentId);
        return ApiResponse.ok("Hủy phân công nhân viên khỏi khu vực thành công", null);
    }

    @GetMapping("/my-assigned-areas")
    public ApiResponse<List<AssignedAreaResponse>> getAssignedAreasForCurrentStaff(
            @PathVariable Long farmId) {
        Long currentUserId = securityUtils.getRequiredCurrentUserId();
        List<AssignedAreaResponse> list = areaAssignmentService.getAssignedAreasForCurrentStaff(currentUserId, farmId);
        return ApiResponse.ok("Danh sách khu vực được phân công phụ trách", list);
    }

    @GetMapping("/area-assignments")
    public ApiResponse<List<AssignmentResponse>> getAreaAssignments(
            @PathVariable Long farmId,
            @RequestParam(required = false) Long productionAreaId) {
        Long currentUserId = securityUtils.getRequiredCurrentUserId();
        List<AssignmentResponse> list = areaAssignmentService.getAreaAssignments(currentUserId, farmId, productionAreaId);
        return ApiResponse.ok("Danh sách phân công khu vực sản xuất", list);
    }
}
