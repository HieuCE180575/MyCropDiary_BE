package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.farm.*;
import com.mycropdiary.api.service.FarmService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller quản lý REST API cho module Farm Core (Trang trại, Thành viên, Vùng sản xuất).
 */
@RestController
@RequestMapping("/api/v1/farms")
@Tag(name = "02. Trang trại (Farms)", description = "APIs quản lý trang trại, thành viên và vùng sản xuất")
@SecurityRequirement(name = "BearerAuthentication")
public class FarmController {
    private final FarmService farmService;
    private final SecurityUtils securityUtils;

    public FarmController(FarmService farmService, SecurityUtils securityUtils) {
        this.farmService = farmService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Tạo mới trang trại", description = "Tạo mới trang trại. Người tạo sẽ tự động được cấp quyền OWNER.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<FarmResp> createFarm(@Valid @RequestBody CreateFarmReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        FarmResp response = farmService.createFarm(currentUserId, request);
        return ApiResp.ok("Farm created successfully", response);
    }

    @Operation(summary = "Lấy chi tiết trang trại", description = "Lấy chi tiết thông tin trang trại theo ID.")
    @GetMapping("/{farmId}")
    public ApiResp<FarmResp> getFarmById(@PathVariable Integer farmId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        FarmResp response = farmService.getFarmById(currentUserId, farmId);
        return ApiResp.ok("Farm details retrieved successfully", response);
    }

    @Operation(summary = "Tìm kiếm trang trại", description = "Tìm kiếm trang trại có phân trang theo từ khóa, tỉnh thành, trạng thái.")
    @GetMapping
    public ApiResp<PageResp<FarmSummaryResp>> searchFarms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minArea,
            @RequestParam(required = false) BigDecimal maxArea,
            @PageableDefault(size = 10) Pageable pageable) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        PageResp<FarmSummaryResp> response = farmService.searchFarms(
                currentUserId, keyword, province, status, minArea, maxArea, pageable
        );
        return ApiResp.ok("Farms retrieved successfully", response);
    }

    @Operation(summary = "Cập nhật trang trại", description = "Cập nhật thông tin trang trại. Yêu cầu quyền OWNER hoặc ADMIN.")
    @PutMapping("/{farmId}")
    public ApiResp<FarmResp> updateFarm(
            @PathVariable Integer farmId,
            @Valid @RequestBody UpdateFarmReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        FarmResp response = farmService.updateFarm(currentUserId, farmId, request);
        return ApiResp.ok("Farm updated successfully", response);
    }

    @Operation(summary = "Vô hiệu hóa trang trại", description = "Ngừng hoạt động trang trại. Yêu cầu quyền OWNER hoặc ADMIN.")
    @DeleteMapping("/{farmId}")
    public ApiResp<Void> deleteFarm(@PathVariable Integer farmId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        farmService.deleteFarm(currentUserId, farmId);
        return ApiResp.ok("Farm deleted successfully", null);
    }

    @Operation(summary = "Thêm thành viên vào trang trại", description = "Thêm thành viên mới. Yêu cầu quyền OWNER hoặc ADMIN.")
    @PostMapping("/{farmId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<FarmMemberResp> addMember(
            @PathVariable Integer farmId,
            @Valid @RequestBody AddFarmMemberReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        FarmMemberResp response = farmService.addMember(currentUserId, farmId, request);
        return ApiResp.ok("Member added successfully", response);
    }

    @Operation(summary = "Lấy danh sách thành viên trang trại", description = "Lấy toàn bộ thành viên đang hoạt động trong trang trại.")
    @GetMapping("/{farmId}/members")
    public ApiResp<List<FarmMemberResp>> getFarmMembers(@PathVariable Integer farmId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        List<FarmMemberResp> response = farmService.getFarmMembers(currentUserId, farmId);
        return ApiResp.ok("Farm members retrieved successfully", response);
    }

    @Operation(summary = "Xóa thành viên khỏi trang trại", description = "Vô hiệu hóa thành viên. Yêu cầu quyền OWNER hoặc ADMIN.")
    @DeleteMapping("/{farmId}/members/{memberId}")
    public ApiResp<Void> removeMember(
            @PathVariable Integer farmId,
            @PathVariable Integer memberId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        farmService.removeMember(currentUserId, farmId, memberId);
        return ApiResp.ok("Member removed successfully", null);
    }
}
