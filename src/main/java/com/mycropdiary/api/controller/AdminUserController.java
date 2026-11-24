package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.admin.UpdateUserStatusReqDTO;
import com.mycropdiary.api.dto.admin.UserRespDTO;
import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.service.admin.AdminUserService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "03. Quản trị - Quản lý tài khoản (Admin)", description = "APIs cho Admin quản lý danh sách và trạng thái tài khoản người dùng")
@SecurityRequirement(name = "BearerAuthentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final SecurityUtils securityUtils;

    public AdminUserController(AdminUserService adminUserService, SecurityUtils securityUtils) {
        this.adminUserService = adminUserService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Danh sách tài khoản người dùng (Admin)",
            description = "Admin xem danh sách tài khoản trong hệ thống. Có thể lọc theo trạng thái (ACTIVE, LOCKED, PENDING) và tìm kiếm theo tên/email/SĐT.")
    @GetMapping
    public ApiResp<PageResp<UserRespDTO>> getUsers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResp<UserRespDTO> page = adminUserService.getUsers(status, search, pageable);
        return ApiResp.ok("Danh sách tài khoản người dùng", page);
    }

    @Operation(summary = "Chi tiết tài khoản người dùng (Admin)",
            description = "Admin xem chi tiết thông tin tài khoản người dùng theo ID.")
    @GetMapping("/{userId}")
    public ApiResp<UserRespDTO> getUserById(@PathVariable Integer userId) {
        UserRespDTO user = adminUserService.getUserById(userId);
        return ApiResp.ok("Chi tiết tài khoản người dùng", user);
    }

    @Operation(summary = "Cập nhật trạng thái tài khoản (Khóa / Mở khóa)",
            description = "Admin cập nhật trạng thái tài khoản (ACTIVE, LOCKED, PENDING). Chặn việc Admin tự khóa tài khoản của chính mình.")
    @RequestMapping(value = "/{userId}/status", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ApiResp<UserRespDTO> updateUserStatus(
            @PathVariable Integer userId,
            @Valid @RequestBody UpdateUserStatusReqDTO request) {
        Integer adminUserId = securityUtils.getCurrentUserId();
        UserRespDTO updated = adminUserService.updateUserStatus(adminUserId, userId, request);
        String actionMessage = "LOCKED".equalsIgnoreCase(request.status())
                ? "Đã khóa tài khoản người dùng thành công."
                : "Đã cập nhật trạng thái tài khoản thành công.";
        return ApiResp.ok(actionMessage, updated);
    }

    @Operation(summary = "Tạo tài khoản người dùng mới (Admin)",
            description = "Admin tạo tài khoản người dùng mới và tự động gửi email chứa OTP xác thực/kích hoạt tài khoản.")
    @PostMapping
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ApiResp<UserRespDTO> createUser(
            @Valid @RequestBody com.mycropdiary.api.dto.admin.CreateAdminUserReqDTO request) {
        Integer adminUserId = securityUtils.getCurrentUserId();
        UserRespDTO created = adminUserService.createUser(adminUserId, request);
        return ApiResp.ok("Đã tạo tài khoản người dùng và gửi email kích hoạt thành công.", created);
    }

    @Operation(summary = "Chỉnh sửa thông tin tài khoản người dùng (Admin)",
            description = "Admin chỉnh sửa họ tên, số điện thoại, vai trò hệ thống hoặc trạng thái của người dùng.")
    @PutMapping("/{userId}")
    public ApiResp<UserRespDTO> updateUser(
            @PathVariable Integer userId,
            @Valid @RequestBody com.mycropdiary.api.dto.admin.UpdateUserReqDTO request) {
        Integer adminUserId = securityUtils.getCurrentUserId();
        UserRespDTO updated = adminUserService.updateUser(adminUserId, userId, request);
        return ApiResp.ok("Đã cập nhật thông tin người dùng thành công.", updated);
    }
}
