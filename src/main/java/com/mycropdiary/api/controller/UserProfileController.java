package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.user.ChangePasswordReq;
import com.mycropdiary.api.dto.user.UpdateProfileReq;
import com.mycropdiary.api.dto.user.UserProfileResp;
import com.mycropdiary.api.service.UserProfileService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@Tag(name = "01. Hồ sơ cá nhân (User Profile)", description = "APIs xem, cập nhật hồ sơ cá nhân và đổi mật khẩu cho người dùng đã đăng nhập")
@SecurityRequirement(name = "BearerAuthentication")
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final SecurityUtils securityUtils;

    public UserProfileController(UserProfileService userProfileService, SecurityUtils securityUtils) {
        this.userProfileService = userProfileService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Xem thông tin hồ sơ cá nhân", description = "Lấy thông tin chi tiết tài khoản của người dùng đang đăng nhập.")
    @GetMapping("/profile")
    public ApiResp<UserProfileResp> getProfile() {
        Integer currentUserId = securityUtils.getCurrentUserId();
        UserProfileResp response = userProfileService.getProfile(currentUserId);
        return ApiResp.ok("Lấy thông tin tài khoản thành công.", response);
    }

    @Operation(summary = "Cập nhật hồ sơ cá nhân", description = "Chỉnh sửa họ tên, số điện thoại hoặc ảnh đại diện của người dùng hiện tại.")
    @PutMapping("/profile")
    public ApiResp<UserProfileResp> updateProfile(@Valid @RequestBody UpdateProfileReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        UserProfileResp response = userProfileService.updateProfile(currentUserId, request);
        return ApiResp.ok("Cập nhật thông tin tài khoản thành công.", response);
    }

    @Operation(summary = "Đổi mật khẩu người dùng", description = "Đổi mật khẩu tài khoản người dùng đăng nhập bằng mật khẩu hiện tại và mật khẩu mới.")
    @PostMapping("/change-password")
    public ApiResp<Void> changePassword(@Valid @RequestBody ChangePasswordReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        userProfileService.changePassword(currentUserId, request);
        return ApiResp.ok("Đổi mật khẩu thành công. Mật khẩu mới đã được áp dụng.", null);
    }
}
