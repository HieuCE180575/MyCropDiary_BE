package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.cultivation.CreateCropCategoryReqDTO;
import com.mycropdiary.api.dto.cultivation.CropCategoryRespDTO;
import com.mycropdiary.api.dto.cultivation.UpdateCropCategoryReqDTO;
import com.mycropdiary.api.dto.cultivation.UpdateCropCategoryStatusReqDTO;
import com.mycropdiary.api.service.admin.CropCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/crop-categories")
@Tag(name = "03. Quản trị - Danh mục mùa vụ & cây trồng (Admin)", description = "APIs quản lý danh mục cây trồng nông nghiệp dùng chung")
@SecurityRequirement(name = "BearerAuthentication")
@PreAuthorize("hasRole('ADMIN')")
public class CropCategoryController {

    private final CropCategoryService cropCategoryService;

    public CropCategoryController(CropCategoryService cropCategoryService) {
        this.cropCategoryService = cropCategoryService;
    }

    @Operation(summary = "Xem danh mục cây trồng mùa vụ (Admin)", description = "Lấy danh sách toàn bộ các loại cây trồng dùng chung")
    @GetMapping
    public ApiResp<List<CropCategoryRespDTO>> getCropCategories() {
        return ApiResp.ok("Danh mục cây trồng mùa vụ", cropCategoryService.getCropCategories());
    }

    @Operation(summary = "Thêm mới danh mục cây trồng (Admin)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<CropCategoryRespDTO> createCropCategory(@RequestBody CreateCropCategoryReqDTO request) {
        return ApiResp.ok("Tạo danh mục cây trồng thành công", cropCategoryService.createCropCategory(request));
    }

    @Operation(summary = "Cập nhật danh mục cây trồng (Admin)")
    @PutMapping("/{id}")
    public ApiResp<CropCategoryRespDTO> updateCropCategory(@PathVariable Integer id, @RequestBody UpdateCropCategoryReqDTO request) {
        return ApiResp.ok("Cập nhật danh mục cây trồng thành công", cropCategoryService.updateCropCategory(id, request));
    }

    @Operation(summary = "Cập nhật trạng thái danh mục cây trồng (Admin)")
    @PatchMapping("/{id}/status")
    public ApiResp<CropCategoryRespDTO> updateCropCategoryStatus(@PathVariable Integer id, @RequestBody UpdateCropCategoryStatusReqDTO request) {
        return ApiResp.ok("Cập nhật trạng thái danh mục thành công", cropCategoryService.updateCropCategoryStatus(id, request));
    }
}
