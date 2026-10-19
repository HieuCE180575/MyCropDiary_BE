package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.assignment.AssignedZoneResponse;
import com.mycropdiary.api.dto.assignment.AssignmentResponse;
import com.mycropdiary.api.dto.assignment.CreateAssignmentRequest;
import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.service.ZoneAssignmentService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Zone Assignment Management", description = "Quản lý phân công nhân viên vào Vùng canh tác (Zone)")
@RestController
@RequestMapping("/api/v1/farms/{farmId}")
public class ZoneAssignmentController {
    private final ZoneAssignmentService zoneAssignmentService;
    private final SecurityUtils securityUtils;

    public ZoneAssignmentController(ZoneAssignmentService zoneAssignmentService, SecurityUtils securityUtils) {
        this.zoneAssignmentService = zoneAssignmentService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Phân công nhân viên vào Vùng (Zone)")
    @PostMapping({"/zone-assignments", "/area-assignments"})
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AssignmentResponse> assignStaffToZone(
            @PathVariable UUID farmId,
            @Valid @RequestBody CreateAssignmentRequest request) {
        UUID currentUserId = securityUtils.getRequiredCurrentUserId();
        AssignmentResponse response = zoneAssignmentService.assignStaffToZone(currentUserId, farmId, request);
        return ApiResponse.ok("Phân công nhân viên vào vùng canh tác thành công", response);
    }

    @Operation(summary = "Hủy phân công nhân viên khỏi Vùng (Zone)")
    @DeleteMapping({"/zone-assignments/{assignmentId}", "/area-assignments/{assignmentId}"})
    public ApiResponse<Void> unassignStaffFromZone(
            @PathVariable UUID farmId,
            @PathVariable UUID assignmentId) {
        UUID currentUserId = securityUtils.getRequiredCurrentUserId();
        zoneAssignmentService.unassignStaffFromZone(currentUserId, farmId, assignmentId);
        return ApiResponse.ok("Hủy phân công nhân viên khỏi vùng canh tác thành công", null);
    }

    @Operation(summary = "Lấy danh sách các Vùng được phân công của nhân viên hiện tại")
    @GetMapping({"/my-assigned-zones", "/my-assigned-areas"})
    public ApiResponse<List<AssignedZoneResponse>> getAssignedZonesForCurrentStaff(
            @PathVariable UUID farmId) {
        UUID currentUserId = securityUtils.getRequiredCurrentUserId();
        List<AssignedZoneResponse> list = zoneAssignmentService.getAssignedZonesForCurrentStaff(currentUserId, farmId);
        return ApiResponse.ok("Danh sách vùng được phân công phụ trách", list);
    }

    @Operation(summary = "Lấy danh sách phân công nhân viên theo Vùng")
    @GetMapping({"/zone-assignments", "/area-assignments"})
    public ApiResponse<List<AssignmentResponse>> getZoneAssignments(
            @PathVariable UUID farmId,
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) UUID productionAreaId) {
        UUID currentUserId = securityUtils.getRequiredCurrentUserId();
        UUID targetZoneId = zoneId != null ? zoneId : productionAreaId;
        List<AssignmentResponse> list = zoneAssignmentService.getZoneAssignments(currentUserId, farmId, targetZoneId);
        return ApiResponse.ok("Danh sách phân công vùng sản xuất", list);
    }
}
