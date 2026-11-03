package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.notification.NotificationListResp;
import com.mycropdiary.api.dto.notification.NotificationResp;
import com.mycropdiary.api.service.NotificationService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "10. Thông báo hệ thống (Notifications - UC-10)", description = "APIs xem và quản lý thông báo của người dùng")
@SecurityRequirement(name = "BearerAuthentication")
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    public NotificationController(NotificationService notificationService, SecurityUtils securityUtils) {
        this.notificationService = notificationService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Xem danh sách thông báo (UC-10: View Notification)", description = "Lấy danh sách thông báo gần đây và số lượng chưa đọc của tài khoản hiện tại.")
    @GetMapping
    public ApiResp<NotificationListResp> getMyNotifications(@RequestParam(defaultValue = "20") int limit) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        NotificationListResp resp = notificationService.getMyNotifications(currentUserId, limit);
        return ApiResp.ok("Lấy danh sách thông báo thành công.", resp);
    }

    @Operation(summary = "Đánh dấu thông báo đã đọc", description = "Cập nhật trạng thái một thông báo thành đã đọc.")
    @PatchMapping("/{notificationId}/read")
    public ApiResp<NotificationResp> markAsRead(@PathVariable Integer notificationId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        NotificationResp resp = notificationService.markAsRead(currentUserId, notificationId);
        return ApiResp.ok("Đã đánh dấu thông báo đã đọc.", resp);
    }

    @Operation(summary = "Đánh dấu tất cả thông báo đã đọc", description = "Đánh dấu toàn bộ thông báo chưa đọc của tài khoản hiện tại thành đã đọc.")
    @PatchMapping("/read-all")
    public ApiResp<Void> markAllAsRead() {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        notificationService.markAllAsRead(currentUserId);
        return ApiResp.ok("Đã đánh dấu tất cả thông báo là đã đọc.", null);
    }
}

