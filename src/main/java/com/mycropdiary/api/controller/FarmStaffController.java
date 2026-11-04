package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.staff.AddStaffReq;
import com.mycropdiary.api.dto.staff.CandidateUserResp;
import com.mycropdiary.api.dto.staff.FarmMemberResp;
import com.mycropdiary.api.dto.staff.UpdateStaffReq;
import com.mycropdiary.api.service.FarmStaffService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "06. Quản lý nhân sự trang trại (Farm Staff)", description = "Quản lý danh sách thành viên, kỹ sư và nhân sự trang trại")
@SecurityRequirement(name = "BearerAuthentication")
@RestController
@RequestMapping("/api/v1/farms/{farmId}/members")
public class FarmStaffController {
    private final FarmStaffService farmStaffService;
    private final SecurityUtils securityUtils;

    public FarmStaffController(FarmStaffService farmStaffService, SecurityUtils securityUtils) {
        this.farmStaffService = farmStaffService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Xem danh sách nhân viên trang trại", description = "Lấy danh sách nhân sự của trang trại kèm trạng thái và vai trò")
    @GetMapping("/staff")
    public ApiResp<List<FarmMemberResp>> getStaffList(
            @PathVariable Integer farmId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<FarmMemberResp> list = farmStaffService.getStaffList(currentUserId, farmId, status, keyword);
        return ApiResp.ok("Danh sách nhân viên trang trại", list);
    }

    @Operation(summary = "Tìm kiếm người dùng ứng viên", description = "Tìm kiếm tài khoản người dùng có thể mời tham gia trang trại")
    @GetMapping("/candidates")
    public ApiResp<List<CandidateUserResp>> getCandidateUsers(
            @PathVariable Integer farmId,
            @RequestParam(required = false) String keyword) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<CandidateUserResp> list = farmStaffService.getCandidateUsers(currentUserId, farmId, keyword);
        return ApiResp.ok("Danh sách ứng viên tài khoản", list);
    }

    @Operation(summary = "Thêm nhân viên vào trang trại", description = "Gán tài khoản người dùng làm nhân sự trang trại")
    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<FarmMemberResp> addStaff(
            @PathVariable Integer farmId,
            @Valid @RequestBody AddStaffReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        FarmMemberResp response = farmStaffService.addStaff(currentUserId, farmId, request);
        return ApiResp.ok("Thêm nhân viên vào trang trại thành công", response);
    }

    @Operation(summary = "Cập nhật thông tin nhân sự", description = "Cập nhật vai trò, vị trí công việc hoặc trạng thái của nhân viên")
    @PutMapping("/{memberId}")
    public ApiResp<FarmMemberResp> updateStaff(
            @PathVariable Integer farmId,
            @PathVariable Integer memberId,
            @Valid @RequestBody UpdateStaffReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        FarmMemberResp response = farmStaffService.updateStaff(currentUserId, farmId, memberId, request);
        return ApiResp.ok("Cập nhật thông tin nhân viên thành công", response);
    }

    @Operation(summary = "Vô hiệu hóa nhân sự", description = "Tạm dừng quyền truy cập của nhân viên vào trang trại")
    @PatchMapping("/{memberId}/deactivate")
    public ApiResp<Void> deactivateStaff(
            @PathVariable Integer farmId,
            @PathVariable Integer memberId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        farmStaffService.deactivateStaff(currentUserId, farmId, memberId);
        return ApiResp.ok("Đã vô hiệu hóa thành viên thành công", null);
    }

    @Operation(summary = "Xóa nhân sự khỏi trang trại", description = "Xóa hoàn toàn nhân viên khỏi danh sách thành viên trang trại")
    @DeleteMapping("/{memberId}")
    public ApiResp<Void> deleteStaff(
            @PathVariable Integer farmId,
            @PathVariable Integer memberId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        farmStaffService.deleteStaff(currentUserId, farmId, memberId);
        return ApiResp.ok("Đã xóa thành viên khỏi trang trại", null);
    }
}
