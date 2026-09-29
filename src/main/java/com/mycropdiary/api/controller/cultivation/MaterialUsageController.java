package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.cultivation.CreateMaterialUsageRequest;
import com.mycropdiary.api.dto.cultivation.MaterialUsageResponse;
import com.mycropdiary.api.service.cultivation.MaterialUsageService;
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
 * Controller quản lý REST API cho Nhật ký sử dụng vật tư (UC-25).
 */
@RestController
@RequestMapping("/api/v1/material-usages")
@Tag(name = "05. Sử dụng vật tư (Material Usages)", description = "APIs ghi nhận và xem lịch sử sử dụng vật tư nông nghiệp")
@SecurityRequirement(name = "BearerAuthentication")
public class MaterialUsageController {

    private final MaterialUsageService materialUsageService;
    private final SecurityUtils securityUtils;

    public MaterialUsageController(MaterialUsageService materialUsageService, SecurityUtils securityUtils) {
        this.materialUsageService = materialUsageService;
        this.securityUtils = securityUtils;
    }

    /**
     * Ghi nhận sử dụng vật tư cho một hoạt động canh tác (UC-25).
     */
    @Operation(summary = "Tạo mới nhật ký sử dụng vật tư", description = "Ghi nhận sử dụng vật tư (phân bón, thuốc BVTV, v.v.) cho một hoạt động canh tác.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MaterialUsageResponse> createMaterialUsage(@Valid @RequestBody CreateMaterialUsageRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        MaterialUsageResponse response = materialUsageService.createMaterialUsage(currentUserId, request);
        return ApiResponse.ok("Ghi nhận sử dụng vật tư thành công.", response);
    }

    /**
     * Xem danh sách lịch sử sử dụng vật tư theo Hoạt động canh tác (Có phân trang).
     */
    @Operation(summary = "Xem lịch sử sử dụng vật tư theo hoạt động canh tác", description = "Lấy danh sách vật tư đã sử dụng trong một hoạt động canh tác (có phân trang).")
    @GetMapping
    public ApiResponse<PageResponse<MaterialUsageResponse>> getMaterialUsagesByActivity(
            @RequestParam("farmingActivityId") Long farmingActivityId,
            @PageableDefault(size = 20) Pageable pageable) {
        Long currentUserId = securityUtils.getCurrentUserId();
        PageResponse<MaterialUsageResponse> response = materialUsageService.getMaterialUsagesByActivity(currentUserId, farmingActivityId, pageable);
        return ApiResponse.ok("Lấy danh sách sử dụng vật tư thành công.", response);
    }

    /**
     * Xem chi tiết một bản ghi sử dụng vật tư.
     */
    @Operation(summary = "Xem chi tiết bản ghi sử dụng vật tư", description = "Lấy thông tin chi tiết của bản ghi sử dụng vật tư theo ID.")
    @GetMapping("/{materialUsageId}")
    public ApiResponse<MaterialUsageResponse> getMaterialUsageById(@PathVariable("materialUsageId") Long materialUsageId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        MaterialUsageResponse response = materialUsageService.getMaterialUsageById(currentUserId, materialUsageId);
        return ApiResponse.ok("Lấy chi tiết sử dụng vật tư thành công.", response);
    }
}
