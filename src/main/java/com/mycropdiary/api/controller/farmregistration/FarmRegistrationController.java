package com.mycropdiary.api.controller.farmregistration;

import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationCreateRequest;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationResponse;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationSummaryResponse;
import com.mycropdiary.api.service.farmregistration.FarmRegistrationService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Controller quản lý REST API cho đăng ký trang trại từ phía người dùng (UC-09).
 */
@RestController
@RequestMapping("/api/v1/farm-registrations")
@Tag(name = "03. Đăng ký trang trại (Farm Registration)", description = "APIs gửi và quản lý yêu cầu đăng ký trang trại dành cho người dùng")
@SecurityRequirement(name = "BearerAuthentication")
public class FarmRegistrationController {

    private final FarmRegistrationService farmRegistrationService;
    private final SecurityUtils securityUtils;

    public FarmRegistrationController(FarmRegistrationService farmRegistrationService, SecurityUtils securityUtils) {
        this.farmRegistrationService = farmRegistrationService;
        this.securityUtils = securityUtils;
    }

    /**
     * Gửi yêu cầu đăng ký trang trại mới.
     *
     * @param request Thông tin đăng ký trang trại
     * @return Thông tin đơn đăng ký vừa tạo (Status: PENDING)
     */
    @Operation(summary = "Gửi yêu cầu đăng ký trang trại", description = "Người dùng đã xác thực gửi yêu cầu đăng ký trang trại mới (Trạng thái mặc định: PENDING).")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FarmRegistrationResponse> createRegistration(@Valid @RequestBody FarmRegistrationCreateRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        FarmRegistrationResponse response = farmRegistrationService.createRegistration(currentUserId, request);
        return ApiResponse.ok("Gửi yêu cầu đăng ký trang trại thành công.", response);
    }

    /**
     * Xem danh sách các yêu cầu đăng ký trang trại của người dùng đang đăng nhập (Có phân trang).
     *
     * @param pageable Cấu hình phân trang
     * @return Danh sách các yêu cầu đăng ký của người dùng
     */
    @Operation(summary = "Xem danh sách yêu cầu đăng ký của tôi", description = "Lấy danh sách các yêu cầu đăng ký trang trại của người dùng đang đăng nhập (Có phân trang).")
    @GetMapping
    public ApiResponse<PageResponse<FarmRegistrationSummaryResponse>> getMyRegistrations(
            @PageableDefault(size = 20) Pageable pageable) {
        Long currentUserId = securityUtils.getCurrentUserId();
        PageResponse<FarmRegistrationSummaryResponse> response = farmRegistrationService.getMyRegistrations(currentUserId, pageable);
        return ApiResponse.ok("Danh sách yêu cầu đăng ký trang trại của tôi.", response);
    }

    /**
     * Xem chi tiết yêu cầu đăng ký trang trại thuộc sở hữu của người dùng đang đăng nhập.
     *
     * @param registrationId ID đơn đăng ký
     * @return Chi tiết đơn đăng ký
     */
    @Operation(summary = "Xem chi tiết yêu cầu đăng ký của tôi", description = "Lấy thông tin chi tiết một yêu cầu đăng ký trang trại của người dùng đang đăng nhập theo ID.")
    @GetMapping("/{registrationId}")
    public ApiResponse<FarmRegistrationResponse> getMyRegistrationDetail(@PathVariable Long registrationId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        FarmRegistrationResponse response = farmRegistrationService.getMyRegistrationDetail(currentUserId, registrationId);
        return ApiResponse.ok("Chi tiết yêu cầu đăng ký trang trại.", response);
    }

    /**
     * Hủy yêu cầu đăng ký trang trại của chính mình khi trạng thái là PENDING.
     *
     * @param registrationId ID đơn đăng ký
     * @return Chi tiết đơn đăng ký sau khi đã hủy (Status: CANCELLED)
     */
    @Operation(summary = "Hủy yêu cầu đăng ký trang trại", description = "Người dùng hủy yêu cầu đăng ký trang trại của chính mình khi trạng thái là PENDING.")
    @PatchMapping("/{registrationId}/cancel")
    public ApiResponse<FarmRegistrationResponse> cancelRegistration(@PathVariable Long registrationId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        FarmRegistrationResponse response = farmRegistrationService.cancelRegistration(currentUserId, registrationId);
        return ApiResponse.ok("Hủy yêu cầu đăng ký trang trại thành công.", response);
    }
}
