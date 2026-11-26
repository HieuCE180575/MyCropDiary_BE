package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.farm.AdminFarmRegRespDTO;
import com.mycropdiary.api.dto.farm.HandleFarmRegReqDTO;
import com.mycropdiary.api.service.farmregistration.AdminFarmRegService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
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
public class AdminFarmRegController {
    private final AdminFarmRegService farmRegistrationService;
    private final SecurityUtils securityUtils;

    public AdminFarmRegController(AdminFarmRegService farmRegistrationService,
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
    public ApiResp<PageResp<AdminFarmRegRespDTO>> getRegistrations(
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResp<AdminFarmRegRespDTO> page = farmRegistrationService.getRegistrations(status, pageable);
        return ApiResp.ok("Danh sách đơn đăng ký trang trại", page);
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
    public ApiResp<AdminFarmRegRespDTO> getRegistrationById(@PathVariable Integer registrationId) {
        AdminFarmRegRespDTO response = farmRegistrationService.getRegistrationById(registrationId);
        return ApiResp.ok("Chi tiết đơn đăng ký trang trại", response);
    }

    @Operation(summary = "Cập nhật thông tin đơn đăng ký trang trại (Admin)",
            description = "Admin chỉnh sửa thông tin hồ sơ đăng ký trang trại (tên, địa chỉ, tỉnh/huyện/xã, mô tả).")
    @PutMapping("/{registrationId}")
    public ApiResp<AdminFarmRegRespDTO> updateRegistration(
            @PathVariable Integer registrationId,
            @RequestBody Map<String, Object> updates) {
        AdminFarmRegRespDTO response = farmRegistrationService.updateRegistration(registrationId, updates);
        return ApiResp.ok("Đã cập nhật hồ sơ đăng ký trang trại thành công.", response);
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
    public ApiResp<AdminFarmRegRespDTO> handleRegistration(
            @PathVariable Integer registrationId,
            @Valid @RequestBody HandleFarmRegReqDTO request) {
        Integer adminUserId = securityUtils.getCurrentUserId();
        AdminFarmRegRespDTO response = farmRegistrationService.handleRegistration(adminUserId, registrationId, request);
        String actionMessage = "APPROVED".equalsIgnoreCase(request.action())
                ? "Đã duyệt đơn đăng ký và tạo trang trại thành công."
                : "Đã từ chối đơn đăng ký trang trại.";
        return ApiResp.ok(actionMessage, response);
    }
}
