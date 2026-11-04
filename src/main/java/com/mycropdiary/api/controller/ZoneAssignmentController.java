package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.assignment.AssignedZoneResp;
import com.mycropdiary.api.dto.assignment.AssignmentResp;
import com.mycropdiary.api.dto.assignment.CreateAssignmentReq;
import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.service.ZoneAssignmentService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "07. Phân công vùng sản xuất (Zone Assignments)", description = "Quản lý phân công nhân viên vào Vùng canh tác")
@SecurityRequirement(name = "BearerAuthentication")
@RestController
@RequestMapping("/api/v1/farms/{farmId}")
public class ZoneAssignmentController {
    private final ZoneAssignmentService zoneAssignmentService;
    private final SecurityUtils securityUtils;

    public ZoneAssignmentController(ZoneAssignmentService zoneAssignmentService, SecurityUtils securityUtils) {
        this.zoneAssignmentService = zoneAssignmentService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Phân công nhân viên vào Vùng", description = "Phân công nhân viên phụ trách một vùng canh tác")
    @PostMapping("/zone-assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<AssignmentResp> assignStaffToZone(
            @PathVariable Integer farmId,
            @Valid @RequestBody CreateAssignmentReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        AssignmentResp response = zoneAssignmentService.assignStaffToZone(currentUserId, farmId, request);
        return ApiResp.ok("Phân công nhân viên vào vùng canh tác thành công", response);
    }

    @Operation(summary = "Hủy phân công nhân viên khỏi Vùng", description = "Xóa phân công nhân viên phụ trách vùng")
    @DeleteMapping("/zone-assignments/{assignmentId}")
    public ApiResp<Void> unassignStaffFromZone(
            @PathVariable Integer farmId,
            @PathVariable Integer assignmentId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        zoneAssignmentService.unassignStaffFromZone(currentUserId, farmId, assignmentId);
        return ApiResp.ok("Hủy phân công nhân viên khỏi vùng canh tác thành công", null);
    }

    @Operation(summary = "Lấy danh sách các Vùng được phân công của nhân viên hiện tại")
    @GetMapping("/my-assigned-zones")
    public ApiResp<List<AssignedZoneResp>> getAssignedZonesForCurrentStaff(
            @PathVariable Integer farmId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<AssignedZoneResp> list = zoneAssignmentService.getAssignedZonesForCurrentStaff(currentUserId, farmId);
        return ApiResp.ok("Danh sách vùng được phân công phụ trách", list);
    }

    @Operation(summary = "Lấy danh sách phân công nhân viên theo Vùng", description = "Xem danh sách phân công nhân viên trong các vùng canh tác")
    @GetMapping("/zone-assignments")
    public ApiResp<List<AssignmentResp>> getZoneAssignments(
            @PathVariable Integer farmId,
            @RequestParam(required = false) Integer zoneId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<AssignmentResp> list = zoneAssignmentService.getZoneAssignments(currentUserId, farmId, zoneId);
        return ApiResp.ok("Danh sách phân công vùng sản xuất", list);
    }
}
