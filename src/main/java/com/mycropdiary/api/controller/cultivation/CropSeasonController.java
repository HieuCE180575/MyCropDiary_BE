package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateCropSeasonReq;
import com.mycropdiary.api.dto.cultivation.CropCategoryResp;
import com.mycropdiary.api.dto.cultivation.CropSeasonResp;
import com.mycropdiary.api.dto.cultivation.UpdateCropSeasonReq;
import com.mycropdiary.api.dto.cultivation.UpdateCropSeasonStatusReq;
import com.mycropdiary.api.service.cultivation.CropSeasonService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "04. Mùa vụ canh tác (Crop Seasons)", description = "APIs quản lý chu kỳ mùa vụ canh tác của trang trại")
@SecurityRequirement(name = "BearerAuthentication")
public class CropSeasonController {

    private final CropSeasonService cropSeasonService;
    private final SecurityUtils securityUtils;

    public CropSeasonController(CropSeasonService cropSeasonService, SecurityUtils securityUtils) {
        this.cropSeasonService = cropSeasonService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Tạo mới mùa vụ canh tác", description = "Tạo mùa vụ canh tác mới cho một trang trại (chỉ Chủ/Quản lý trang trại).")
    @PostMapping("/farms/{farmId}/crop-seasons")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<CropSeasonResp> createCropSeason(
            @PathVariable Integer farmId,
            @Valid @RequestBody CreateCropSeasonReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        CropSeasonResp response = cropSeasonService.createCropSeason(currentUserId, farmId, request);
        return ApiResp.ok("Tạo mùa vụ canh tác thành công.", response);
    }

    @Operation(summary = "Danh sách mùa vụ của trang trại", description = "Xem danh sách các mùa vụ của trang trại (có phân trang, tìm kiếm theo tên và lọc theo trạng thái/loại cây trồng).")
    @GetMapping("/farms/{farmId}/crop-seasons")
    public ApiResp<PageResp<CropSeasonResp>> getCropSeasons(
            @PathVariable Integer farmId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer cropCategoryId,
            @PageableDefault(size = 15, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        PageResp<CropSeasonResp> response = cropSeasonService.getCropSeasons(currentUserId, farmId, keyword, status, cropCategoryId, pageable);
        return ApiResp.ok("Danh sách mùa vụ canh tác.", response);
    }

    @Operation(summary = "Xem chi tiết mùa vụ canh tác", description = "Lấy thông tin chi tiết của một mùa vụ theo ID.")
    @GetMapping("/crop-seasons/{cropSeasonId}")
    public ApiResp<CropSeasonResp> getCropSeasonById(@PathVariable Integer cropSeasonId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        CropSeasonResp response = cropSeasonService.getCropSeasonById(currentUserId, cropSeasonId);
        return ApiResp.ok("Chi tiết mùa vụ canh tác.", response);
    }

    @Operation(summary = "Cập nhật thông tin mùa vụ", description = "Chỉnh sửa tên vụ, loại cây, ngày bắt đầu/kết thúc hoặc ghi chú (không áp dụng cho vụ đã kết thúc/hủy).")
    @PutMapping("/crop-seasons/{cropSeasonId}")
    public ApiResp<CropSeasonResp> updateCropSeason(
            @PathVariable Integer cropSeasonId,
            @Valid @RequestBody UpdateCropSeasonReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        CropSeasonResp response = cropSeasonService.updateCropSeason(currentUserId, cropSeasonId, request);
        return ApiResp.ok("Cập nhật thông tin mùa vụ thành công.", response);
    }

    @Operation(summary = "Cập nhật trạng thái mùa vụ", description = "Cập nhật trạng thái mùa vụ: planned, active, completed, cancelled. Khi chuyển completed sẽ tự gán ngày kết thúc nếu chưa có.")
    @PatchMapping("/crop-seasons/{cropSeasonId}/status")
    public ApiResp<CropSeasonResp> updateCropSeasonStatus(
            @PathVariable Integer cropSeasonId,
            @Valid @RequestBody UpdateCropSeasonStatusReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        CropSeasonResp response = cropSeasonService.updateCropSeasonStatus(currentUserId, cropSeasonId, request);
        return ApiResp.ok("Cập nhật trạng thái mùa vụ thành công.", response);
    }

    @Operation(summary = "Xóa mùa vụ canh tác", description = "Xóa mùa vụ nếu chưa phát sinh nhật ký canh tác hoặc thu hoạch liên kết.")
    @DeleteMapping("/crop-seasons/{cropSeasonId}")
    public ApiResp<Void> deleteCropSeason(@PathVariable Integer cropSeasonId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        cropSeasonService.deleteCropSeason(currentUserId, cropSeasonId);
        return ApiResp.ok("Xóa mùa vụ canh tác thành công.", null);
    }

    @Operation(summary = "Danh mục loại cây trồng", description = "Lấy danh mục các loại cây trồng (dưa lưới, cà chua, sầu riêng...) để người dùng chọn khi tạo mùa vụ.")
    @GetMapping("/crop-categories")
    public ApiResp<List<CropCategoryResp>> getCropCategories() {
        List<CropCategoryResp> categories = cropSeasonService.getCropCategories();
        return ApiResp.ok("Danh mục loại cây trồng.", categories);
    }
}

