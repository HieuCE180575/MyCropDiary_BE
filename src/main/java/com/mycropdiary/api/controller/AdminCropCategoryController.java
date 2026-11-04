package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.cultivation.CropCategoryResp;
import com.mycropdiary.api.entity.cultivation.CropCategory;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.cultivation.CropCategoryRepository;
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
public class AdminCropCategoryController {

    private final CropCategoryRepository cropCategoryRepository;

    public AdminCropCategoryController(CropCategoryRepository cropCategoryRepository) {
        this.cropCategoryRepository = cropCategoryRepository;
    }

    @Operation(summary = "Xem danh mục cây trồng mùa vụ (Admin)", description = "Lấy danh sách toàn bộ các loại cây trồng dùng chung")
    @GetMapping
    public ApiResp<List<CropCategoryResp>> getCropCategories() {
        List<CropCategoryResp> list = cropCategoryRepository.findAll().stream()
                .map(c -> new CropCategoryResp(c.getId(), c.getCategoryName(), c.getDescription(), c.getImageUrl()))
                .toList();
        return ApiResp.ok("Danh mục cây trồng mùa vụ", list);
    }

    @Operation(summary = "Thêm mới danh mục cây trồng (Admin)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<CropCategoryResp> createCropCategory(@RequestBody CropCategoryReq request) {
        CropCategory category = new CropCategory(request.categoryName(), request.description());
        category.setImageUrl(request.imageUrl());
        CropCategory saved = cropCategoryRepository.save(category);
        return ApiResp.ok("Tạo danh mục cây trồng thành công",
                new CropCategoryResp(saved.getId(), saved.getCategoryName(), saved.getDescription(), saved.getImageUrl()));
    }

    @Operation(summary = "Cập nhật danh mục cây trồng (Admin)")
    @PutMapping("/{id}")
    public ApiResp<CropCategoryResp> updateCropCategory(@PathVariable Integer id, @RequestBody CropCategoryReq request) {
        CropCategory category = cropCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục ID: " + id));
        category.setCategoryName(request.categoryName());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        CropCategory saved = cropCategoryRepository.save(category);
        return ApiResp.ok("Cập nhật danh mục cây trồng thành công",
                new CropCategoryResp(saved.getId(), saved.getCategoryName(), saved.getDescription(), saved.getImageUrl()));
    }

    public record CropCategoryReq(String categoryName, String description, String imageUrl) {}
}

