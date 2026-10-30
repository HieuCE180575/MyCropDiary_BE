package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.staff.FarmInvitationResp;
import com.mycropdiary.api.dto.staff.RespondInvitationReq;
import com.mycropdiary.api.service.FarmStaffService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user/farm-invitations")
@Tag(name = "01b. Lời mời tham gia trang trại (Farm Invitations)", description = "APIs xem danh sách lời mời và phê duyệt (Approve/Reject) tham gia trang trại")
@SecurityRequirement(name = "BearerAuthentication")
public class FarmInvitationController {

    private final FarmStaffService farmStaffService;
    private final SecurityUtils securityUtils;

    public FarmInvitationController(FarmStaffService farmStaffService, SecurityUtils securityUtils) {
        this.farmStaffService = farmStaffService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Xem danh sách lời mời tham gia trang trại",
            description = "Lấy danh sách các lời mời vào trang trại (với vai trò Staff hoặc Co-Owner) đang chờ tài khoản hiện tại phản hồi.")
    @GetMapping
    public ApiResp<List<FarmInvitationResp>> getPendingInvitations() {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<FarmInvitationResp> invitations = farmStaffService.getPendingInvitations(currentUserId);
        return ApiResp.ok("Danh sách lời mời tham gia trang trại", invitations);
    }

    @Operation(summary = "Chấp nhận lời mời tham gia trang trại",
            description = "Người dùng chấp thuận lời mời gia nhập trang trại. Trạng thái thành viên chuyển sang ACTIVE.")
    @PostMapping("/{memberId}/approve")
    public ApiResp<Void> approveInvitation(@PathVariable Integer memberId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        farmStaffService.respondInvitation(currentUserId, memberId, "approve");
        return ApiResp.ok("Đã chấp nhận lời mời tham gia trang trại thành công", null);
    }

    @Operation(summary = "Từ chối lời mời tham gia trang trại",
            description = "Người dùng từ chối lời mời gia nhập trang trại.")
    @PostMapping("/{memberId}/reject")
    public ApiResp<Void> rejectInvitation(@PathVariable Integer memberId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        farmStaffService.respondInvitation(currentUserId, memberId, "reject");
        return ApiResp.ok("Đã từ chối lời mời tham gia trang trại", null);
    }

    @Operation(summary = "Phản hồi lời mời (Approve hoặc Reject)",
            description = "Endpoint linh hoạt cho phép truyền action qua request body.")
    @PostMapping("/{memberId}/respond")
    public ApiResp<Void> respondInvitation(
            @PathVariable Integer memberId,
            @Valid @RequestBody RespondInvitationReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        farmStaffService.respondInvitation(currentUserId, memberId, request.action());
        String msg = "approve".equalsIgnoreCase(request.action())
                ? "Đã chấp nhận lời mời tham gia trang trại thành công"
                : "Đã từ chối lời mời tham gia trang trại";
        return ApiResp.ok(msg, null);
    }
}

