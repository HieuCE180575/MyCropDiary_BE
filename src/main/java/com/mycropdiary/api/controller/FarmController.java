package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.dto.common.PageResponse;
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
import java.util.UUID;

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
    public ApiResponse<FarmResponse> createFarm(@Valid @RequestBody CreateFarmRequest request) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        FarmResponse response = farmService.createFarm(currentUserId, request);
        return ApiResponse.ok("Farm created successfully", response);
    }

    @Operation(summary = "Lấy chi tiết trang trại", description = "Lấy chi tiết thông tin trang trại theo ID.")
    @GetMapping("/{farmId}")
    public ApiResponse<FarmResponse> getFarmById(@PathVariable UUID farmId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        FarmResponse response = farmService.getFarmById(currentUserId, farmId);
        return ApiResponse.ok("Farm details retrieved successfully", response);
    }

    @Operation(summary = "Tìm kiếm trang trại", description = "Tìm kiếm trang trại có phân trang theo từ khóa, tỉnh thành, trạng thái.")
    @GetMapping
    public ApiResponse<PageResponse<FarmSummaryResponse>> searchFarms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minArea,
            @RequestParam(required = false) BigDecimal maxArea,
            @PageableDefault(size = 10) Pageable pageable) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        PageResponse<FarmSummaryResponse> response = farmService.searchFarms(
                currentUserId, keyword, province, status, minArea, maxArea, pageable
        );
        return ApiResponse.ok("Farms retrieved successfully", response);
    }

    @Operation(summary = "Cập nhật trang trại", description = "Cập nhật thông tin trang trại. Yêu cầu quyền OWNER hoặc ADMIN.")
    @PutMapping("/{farmId}")
    public ApiResponse<FarmResponse> updateFarm(
            @PathVariable UUID farmId,
            @Valid @RequestBody UpdateFarmRequest request) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        FarmResponse response = farmService.updateFarm(currentUserId, farmId, request);
        return ApiResponse.ok("Farm updated successfully", response);
    }

    @Operation(summary = "Vô hiệu hóa trang trại", description = "Ngừng hoạt động trang trại. Yêu cầu quyền OWNER hoặc ADMIN.")
    @DeleteMapping("/{farmId}")
    public ApiResponse<Void> deleteFarm(@PathVariable UUID farmId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        farmService.deleteFarm(currentUserId, farmId);
        return ApiResponse.ok("Farm deleted successfully", null);
    }

    @Operation(summary = "Thêm thành viên vào trang trại", description = "Thêm thành viên mới. Yêu cầu quyền OWNER hoặc ADMIN.")
    @PostMapping("/{farmId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FarmMemberResponse> addMember(
            @PathVariable UUID farmId,
            @Valid @RequestBody AddFarmMemberRequest request) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        FarmMemberResponse response = farmService.addMember(currentUserId, farmId, request);
        return ApiResponse.ok("Member added successfully", response);
    }

    @Operation(summary = "Lấy danh sách thành viên trang trại", description = "Lấy toàn bộ thành viên đang hoạt động trong trang trại.")
    @GetMapping("/{farmId}/members")
    public ApiResponse<List<FarmMemberResponse>> getFarmMembers(@PathVariable UUID farmId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        List<FarmMemberResponse> response = farmService.getFarmMembers(currentUserId, farmId);
        return ApiResponse.ok("Farm members retrieved successfully", response);
    }

    @Operation(summary = "Xóa thành viên khỏi trang trại", description = "Vô hiệu hóa thành viên. Yêu cầu quyền OWNER hoặc ADMIN.")
    @DeleteMapping("/{farmId}/members/{memberId}")
    public ApiResponse<Void> removeMember(
            @PathVariable UUID farmId,
            @PathVariable UUID memberId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        farmService.removeMember(currentUserId, farmId, memberId);
        return ApiResponse.ok("Member removed successfully", null);
    }

    @Operation(summary = "Tạo vùng sản xuất / Zone mới", description = "Tạo phân khu sản xuất mới trong trang trại.")
    @PostMapping({"/{farmId}/zones", "/{farmId}/production-areas"})
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ZoneResponse> createZone(
            @PathVariable UUID farmId,
            @Valid @RequestBody CreateZoneRequest request) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        ZoneResponse response = farmService.createZone(currentUserId, farmId, request);
        return ApiResponse.ok("Zone created successfully", response);
    }

    @Operation(summary = "Lấy danh sách vùng sản xuất / Zones", description = "Lấy danh sách các vùng sản xuất thuộc trang trại.")
    @GetMapping({"/{farmId}/zones", "/{farmId}/production-areas"})
    public ApiResponse<List<ZoneResponse>> getZones(@PathVariable UUID farmId) {
        UUID currentUserId = securityUtils.getCurrentUserId();
        List<ZoneResponse> response = farmService.getZones(currentUserId, farmId);
        return ApiResponse.ok("Zones retrieved successfully", response);
    }
}
