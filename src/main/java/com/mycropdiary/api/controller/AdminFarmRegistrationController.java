package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.farm.FarmRegistrationResponse;
import com.mycropdiary.api.dto.farm.HandleRegistrationRequest;
import com.mycropdiary.api.service.farmregistration.FarmRegistrationService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// [AI_CHANGE] Root cause: UC-39 Admin cần REST API để xem danh sách, chi tiết và duyệt/từ chối đơn đăng ký farm
// [AI_CHANGE] Mechanism: Controller riêng cho Admin với prefix /api/v1/admin/farm-registrations,
//             sử dụng @PreAuthorize("hasRole('ADMIN')") để đảm bảo chỉ Admin mới truy cập được
@RestController
@RequestMapping("/api/v1/admin/farm-registrations")
@Tag(name = "03. Quản trị - Đơn đăng ký trang trại (Admin)", description = "APIs cho Admin duyệt/từ chối đơn đăng ký trang trại")
@SecurityRequirement(name = "BearerAuthentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFarmRegistrationController {
    private final FarmRegistrationService farmRegistrationService;
    private final SecurityUtils securityUtils;

    public AdminFarmRegistrationController(FarmRegistrationService farmRegistrationService,
                                           SecurityUtils securityUtils) {
        this.farmRegistrationService = farmRegistrationService;
        this.securityUtils = securityUtils;
    }

    /**
     * UC-39: Admin xem danh sách đơn đăng ký trang trại (phân trang, lọc theo trạng thái).
     *
     * @param status Lọc theo trạng thái (PENDING, APPROVED, REJECTED, CANCELLED), null = tất cả
     * @param pageable Cấu hình phân trang (page, size, sort)
     * @return Danh sách đơn đăng ký phân trang
     */
    @Operation(summary = "Danh sách đơn đăng ký trang trại (UC-39)",
            description = "Admin xem danh sách đơn đăng ký trang trại. Có thể lọc theo trạng thái PENDING, APPROVED, REJECTED, CANCELLED.")
    @GetMapping
    public ApiResponse<PageResponse<FarmRegistrationResponse>> getRegistrations(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "submittedAt") Pageable pageable) {
        PageResponse<FarmRegistrationResponse> page = farmRegistrationService.getRegistrations(status, pageable);
        return ApiResponse.ok("Danh sách đơn đăng ký trang trại", page);
    }

    /**
     * UC-39: Admin xem chi tiết đơn đăng ký trang trại.
     *
     * @param registrationId ID đơn đăng ký
     * @return Chi tiết đơn đăng ký
     */
    @Operation(summary = "Chi tiết đơn đăng ký trang trại (UC-39)",
            description = "Admin xem chi tiết thông tin đơn đăng ký trang trại bao gồm thông tin người nộp, địa chỉ, trạng thái xử lý.")
    @GetMapping("/{registrationId}")
    public ApiResponse<FarmRegistrationResponse> getRegistrationById(@PathVariable Long registrationId) {
        FarmRegistrationResponse response = farmRegistrationService.getRegistrationById(registrationId);
        return ApiResponse.ok("Chi tiết đơn đăng ký trang trại", response);
    }

    /**
     * UC-39: Admin duyệt hoặc từ chối đơn đăng ký trang trại.
     * Khi duyệt (APPROVED): Tự động tạo Farm mới + gán OWNER cho người nộp đơn.
     * Khi từ chối (REJECTED): Bắt buộc kèm lý do từ chối.
     * Chặn xử lý trùng: Chỉ xử lý đơn có trạng thái PENDING.
     *
     * @param registrationId ID đơn đăng ký
     * @param request Hành động xử lý (action: APPROVED/REJECTED, rejectionReason)
     * @return Đơn đăng ký sau khi xử lý
     */
    @Operation(summary = "Duyệt / Từ chối đơn đăng ký trang trại (UC-39)",
            description = "Admin duyệt (APPROVED) hoặc từ chối (REJECTED) đơn đăng ký. " +
                          "Khi duyệt: tự động tạo Farm + gán quyền OWNER cho người nộp đơn. " +
                          "Khi từ chối: bắt buộc kèm lý do từ chối. " +
                          "Chỉ xử lý đơn ở trạng thái PENDING (chặn xử lý trùng).")
    @PutMapping("/{registrationId}/handle")
    public ApiResponse<FarmRegistrationResponse> handleRegistration(
            @PathVariable Long registrationId,
            @Valid @RequestBody HandleRegistrationRequest request) {
        Long adminUserId = securityUtils.getCurrentUserId();
        FarmRegistrationResponse response = farmRegistrationService.handleRegistration(adminUserId, registrationId, request);
        String actionMessage = "APPROVED".equals(request.action())
                ? "Đã duyệt đơn đăng ký và tạo trang trại thành công."
                : "Đã từ chối đơn đăng ký trang trại.";
        return ApiResponse.ok(actionMessage, response);
    }
}
