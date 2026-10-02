package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.dto.staff.AddStaffRequest;
import com.mycropdiary.api.dto.staff.FarmMemberResponse;
import com.mycropdiary.api.dto.staff.UpdateStaffRequest;
import com.mycropdiary.api.util.SecurityUtils;
import com.mycropdiary.api.service.FarmStaffService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/farms/{farmId}/members")
public class FarmStaffController {
    private final FarmStaffService farmStaffService;
    private final SecurityUtils securityUtils;

    public FarmStaffController(FarmStaffService farmStaffService, SecurityUtils securityUtils) {
        this.farmStaffService = farmStaffService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ApiResponse<List<FarmMemberResponse>> getStaffList(
            @PathVariable Long farmId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Long currentUserId = securityUtils.getRequiredCurrentUserId();
        List<FarmMemberResponse> list = farmStaffService.getStaffList(currentUserId, farmId, status, keyword);
        return ApiResponse.ok("Danh sách nhân viên trang trại", list);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FarmMemberResponse> addStaff(
            @PathVariable Long farmId,
            @Valid @RequestBody AddStaffRequest request) {
        Long currentUserId = securityUtils.getRequiredCurrentUserId();
        FarmMemberResponse response = farmStaffService.addStaff(currentUserId, farmId, request);
        return ApiResponse.ok("Thêm nhân viên vào trang trại thành công", response);
    }

    @PutMapping("/{memberId}")
    public ApiResponse<FarmMemberResponse> updateStaff(
            @PathVariable Long farmId,
            @PathVariable Long memberId,
            @Valid @RequestBody UpdateStaffRequest request) {
        Long currentUserId = securityUtils.getRequiredCurrentUserId();
        FarmMemberResponse response = farmStaffService.updateStaff(currentUserId, farmId, memberId, request);
        return ApiResponse.ok("Cập nhật thông tin nhân viên thành công", response);
    }

    @PatchMapping("/{memberId}/deactivate")
    public ApiResponse<Void> deactivateStaff(
            @PathVariable Long farmId,
            @PathVariable Long memberId) {
        Long currentUserId = securityUtils.getRequiredCurrentUserId();
        farmStaffService.deactivateStaff(currentUserId, farmId, memberId);
        return ApiResponse.ok("Đã ngừng hoạt động nhân viên và đóng tất cả phân công khu vực", null);
    }
}
