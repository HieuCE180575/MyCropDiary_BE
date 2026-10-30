package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.staff.AddStaffReq;
import com.mycropdiary.api.dto.staff.CandidateUserResp;
import com.mycropdiary.api.dto.staff.FarmMemberResp;
import com.mycropdiary.api.dto.staff.UpdateStaffReq;
import com.mycropdiary.api.service.FarmStaffService;
import com.mycropdiary.api.util.SecurityUtils;
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

    @GetMapping("/staff")
    public ApiResp<List<FarmMemberResp>> getStaffList(
            @PathVariable Integer farmId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<FarmMemberResp> list = farmStaffService.getStaffList(currentUserId, farmId, status, keyword);
        return ApiResp.ok("Danh sách nhân viên trang trại", list);
    }

    @GetMapping("/candidates")
    public ApiResp<List<CandidateUserResp>> getCandidateUsers(
            @PathVariable Integer farmId,
            @RequestParam(required = false) String keyword) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<CandidateUserResp> list = farmStaffService.getCandidateUsers(currentUserId, farmId, keyword);
        return ApiResp.ok("Danh sách ứng viên tài khoản", list);
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<FarmMemberResp> addStaff(
            @PathVariable Integer farmId,
            @Valid @RequestBody AddStaffReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        FarmMemberResp response = farmStaffService.addStaff(currentUserId, farmId, request);
        return ApiResp.ok("Thêm nhân viên vào trang trại thành công", response);
    }

    @PutMapping("/{memberId}")
    public ApiResp<FarmMemberResp> updateStaff(
            @PathVariable Integer farmId,
            @PathVariable Integer memberId,
            @Valid @RequestBody UpdateStaffReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        FarmMemberResp response = farmStaffService.updateStaff(currentUserId, farmId, memberId, request);
        return ApiResp.ok("Cập nhật thông tin nhân viên thành công", response);
    }

    @PatchMapping("/{memberId}/deactivate")
    public ApiResp<Void> deactivateStaff(
            @PathVariable Integer farmId,
            @PathVariable Integer memberId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        farmStaffService.deactivateStaff(currentUserId, farmId, memberId);
        return ApiResp.ok("Đã vô hiệu hóa thành viên thành công", null);
    }

    @DeleteMapping("/{memberId}")
    public ApiResp<Void> deleteStaff(
            @PathVariable Integer farmId,
            @PathVariable Integer memberId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        farmStaffService.deleteStaff(currentUserId, farmId, memberId);
        return ApiResp.ok("Đã xóa thành viên khỏi trang trại", null);
    }
}
