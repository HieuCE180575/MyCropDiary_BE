package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateMaterialUsageReq;
import com.mycropdiary.api.dto.cultivation.MaterialUsageResp;
import com.mycropdiary.api.dto.cultivation.UpdateMaterialUsageReq;
import com.mycropdiary.api.service.cultivation.MaterialUsageService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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
    public ApiResp<MaterialUsageResp> createMaterialUsage(@Valid @RequestBody CreateMaterialUsageReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        MaterialUsageResp response = materialUsageService.createMaterialUsage(currentUserId, request);
        return ApiResp.ok("Ghi nhận sử dụng vật tư thành công.", response);
    }

    /**
     * Cập nhật nhật ký sử dụng vật tư (UC-25.3).
     */
    @Operation(summary = "Cập nhật nhật ký sử dụng vật tư", description = "Chỉnh sửa thông tin bản ghi sử dụng vật tư đã tạo (UC-25.3).")
    @PutMapping("/{materialUsageId}")
    public ApiResp<MaterialUsageResp> updateMaterialUsage(
            @PathVariable Integer materialUsageId,
            @Valid @RequestBody UpdateMaterialUsageReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        MaterialUsageResp response = materialUsageService.updateMaterialUsage(currentUserId, materialUsageId, request);
        return ApiResp.ok("Cập nhật sử dụng vật tư thành công.", response);
    }

    /**
     * Xem danh sách lịch sử sử dụng vật tư (Có phân trang, tìm kiếm và lọc).
     */
    @Operation(summary = "Xem lịch sử sử dụng vật tư", description = "Lấy danh sách vật tư đã sử dụng theo hoạt động canh tác hoặc mùa vụ (có phân trang, tìm kiếm theo từ khóa và lọc nâng cao).")
    @GetMapping
    public ApiResp<PageResp<MaterialUsageResp>> getMaterialUsages(
            @RequestParam(required = false) Integer farmingActivityId,
            @RequestParam(required = false) Integer cropSeasonId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String materialType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 20) Pageable pageable) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        PageResp<MaterialUsageResp> response = materialUsageService.searchAndFilterMaterialUsages(
                currentUserId, farmingActivityId, cropSeasonId, keyword, materialType, fromDate, toDate, pageable);
        return ApiResp.ok("Lấy danh sách sử dụng vật tư thành công.", response);
    }

    /**
     * Xem chi tiết một bản ghi sử dụng vật tư.
     */
    @Operation(summary = "Xem chi tiết bản ghi sử dụng vật tư", description = "Lấy thông tin chi tiết của bản ghi sử dụng vật tư theo ID.")
    @GetMapping("/{materialUsageId}")
    public ApiResp<MaterialUsageResp> getMaterialUsageById(@PathVariable("materialUsageId") Integer materialUsageId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        MaterialUsageResp response = materialUsageService.getMaterialUsageById(currentUserId, materialUsageId);
        return ApiResp.ok("Lấy chi tiết sử dụng vật tư thành công.", response);
    }
}
