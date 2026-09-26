package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.farm.*;
import com.mycropdiary.api.service.FarmService;
import com.mycropdiary.api.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/farms")
public class FarmController {
    private final FarmService farmService;
    private final SecurityUtils securityUtils;

    public FarmController(FarmService farmService, SecurityUtils securityUtils) {
        this.farmService = farmService;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FarmResponse> createFarm(@Valid @RequestBody CreateFarmRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        FarmResponse response = farmService.createFarm(currentUserId, request);
        return ApiResponse.ok("Farm created successfully", response);
    }

    @GetMapping("/{farmId}")
    public ApiResponse<FarmResponse> getFarmById(@PathVariable Long farmId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        FarmResponse response = farmService.getFarmById(currentUserId, farmId);
        return ApiResponse.ok("Farm details", response);
    }

    @GetMapping
    public ApiResponse<PageResponse<FarmSummaryResponse>> searchFarms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String province,
            @PageableDefault(size = 20) Pageable pageable) {
        Long currentUserId = securityUtils.getCurrentUserId();
        PageResponse<FarmSummaryResponse> page = farmService.searchFarms(currentUserId, keyword, status, province, pageable);
        return ApiResponse.ok("Accessible farms list", page);
    }

    @PutMapping("/{farmId}")
    public ApiResponse<FarmResponse> updateFarm(
            @PathVariable Long farmId,
            @Valid @RequestBody UpdateFarmRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        FarmResponse response = farmService.updateFarm(currentUserId, farmId, request);
        return ApiResponse.ok("Farm updated successfully", response);
    }

    @DeleteMapping("/{farmId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> deleteFarm(@PathVariable Long farmId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        farmService.deleteFarm(currentUserId, farmId);
        return ApiResponse.ok("Farm deactivated successfully", null);
    }

    @GetMapping("/{farmId}/members")
    public ApiResponse<List<FarmMemberResponse>> getFarmMembers(@PathVariable Long farmId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        List<FarmMemberResponse> members = farmService.getFarmMembers(currentUserId, farmId);
        return ApiResponse.ok("Farm members list", members);
    }

    @PostMapping("/{farmId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FarmMemberResponse> addMember(
            @PathVariable Long farmId,
            @Valid @RequestBody AddFarmMemberRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        FarmMemberResponse member = farmService.addMember(currentUserId, farmId, request);
        return ApiResponse.ok("Farm member added successfully", member);
    }

    @DeleteMapping("/{farmId}/members/{memberId}")
    public ApiResponse<Void> removeMember(
            @PathVariable Long farmId,
            @PathVariable Long memberId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        farmService.removeMember(currentUserId, farmId, memberId);
        return ApiResponse.ok("Farm member removed successfully", null);
    }

    @PostMapping("/{farmId}/members/{memberId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StaffAreaAssignmentResponse> assignStaffToArea(
            @PathVariable Long farmId,
            @PathVariable Long memberId,
            @Valid @RequestBody AssignStaffAreaRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        StaffAreaAssignmentResponse assignment = farmService.assignStaffToArea(currentUserId, farmId, memberId, request);
        return ApiResponse.ok("Staff assigned to production area successfully", assignment);
    }
}
