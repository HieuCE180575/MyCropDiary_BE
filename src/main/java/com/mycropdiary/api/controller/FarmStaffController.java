package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.dto.staff.AddStaffRequest;
import com.mycropdiary.api.dto.staff.FarmMemberResponse;
import com.mycropdiary.api.dto.staff.UpdateStaffRequest;
import com.mycropdiary.api.service.FarmStaffService;
import com.mycropdiary.api.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/farms/{farmId}/members")
public class FarmStaffController {
    private final FarmStaffService farmStaffService;
    private final SecurityUtils securityUtils;

    public FarmStaffController(FarmStaffService farmStaffService, SecurityUtils securityUtils) {
        this.farmStaffService = farmStaffService;
        this.securityUtils = securityUtils;
    }

    @GetMapping("/staff")
    public ApiResponse<List<FarmMemberResponse>> getStaffList(
            @PathVariable UUID farmId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        UUID currentUserId = securityUtils.getRequiredCurrentUserId();
        List<FarmMemberResponse> list = farmStaffService.getStaffList(currentUserId, farmId, status, keyword);
        return ApiResponse.ok("Danh sách nhân viên trang trại", list);
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FarmMemberResponse> addStaff(
            @PathVariable UUID farmId,
            @Valid @RequestBody AddStaffRequest request) {
        UUID currentUserId = securityUtils.getRequiredCurrentUserId();
        FarmMemberResponse response = farmStaffService.addStaff(currentUserId, farmId, request);
        return ApiResponse.ok("Thêm nhân viên vào trang trại thành công", response);
    }

    @PutMapping("/{memberId}")
    public ApiResponse<FarmMemberResponse> updateStaff(
            @PathVariable UUID farmId,
            @PathVariable UUID memberId,
            @Valid @RequestBody UpdateStaffRequest request) {
        UUID currentUserId = securityUtils.getRequiredCurrentUserId();
        FarmMemberResponse response = farmStaffService.updateStaff(currentUserId, farmId, memberId, request);
        return ApiResponse.ok("Cập nhật thông tin nhân viên thành công", response);
    }

    @PatchMapping("/{memberId}/deactivate")
    public ApiResponse<Void> deactivateStaff(
            @PathVariable UUID farmId,
            @PathVariable UUID memberId) {
        UUID currentUserId = securityUtils.getRequiredCurrentUserId();
        farmStaffService.deactivateStaff(currentUserId, farmId, memberId);
        return ApiResponse.ok("Đã vô hiệu hóa thành viên thành công", null);
    }
}
