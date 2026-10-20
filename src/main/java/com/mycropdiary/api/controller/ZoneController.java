package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.assignment.AssignedZoneResp;
import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.farm.CreateZoneReq;
import com.mycropdiary.api.dto.farm.UpdateZoneReq;
import com.mycropdiary.api.dto.farm.ZoneDetailResp;
import com.mycropdiary.api.dto.farm.ZoneResp;
import com.mycropdiary.api.service.ZoneService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "UC-16: Zone Management", description = "Quản lý Phân khu / Vùng sản xuất (Zone)")
@RestController
@RequestMapping("/api/v1")
public class ZoneController {
    private final ZoneService zoneService;
    private final SecurityUtils securityUtils;

    public ZoneController(ZoneService zoneService, SecurityUtils securityUtils) {
        this.zoneService = zoneService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "UC-16: Tạo phân khu (Zone) mới")
    @PostMapping("/farms/{farmId}/zones")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<ZoneResp> createZone(
            @PathVariable Integer farmId,
            @Valid @RequestBody CreateZoneReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        ZoneResp response = zoneService.createZone(currentUserId, farmId, request);
        return ApiResp.ok("Tạo phân khu sản xuất thành công", response);
    }

    @Operation(summary = "UC-16.3, 16.4, 16.5: Xem, Tìm kiếm và Lọc danh sách phân khu (Zone)")
    @GetMapping("/farms/{farmId}/zones")
    public ApiResp<List<ZoneResp>> getZones(
            @PathVariable Integer farmId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<ZoneResp> responses = zoneService.searchZones(currentUserId, farmId, keyword, status);
        return ApiResp.ok("Danh sách phân khu sản xuất", responses);
    }

    @Operation(summary = "UC-16.6: Xem thông tin chi tiết phân khu (Zone)")
    @GetMapping("/zones/{zoneId}")
    public ApiResp<ZoneDetailResp> getZoneById(@PathVariable Integer zoneId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        ZoneDetailResp response = zoneService.getZoneById(currentUserId, zoneId);
        return ApiResp.ok("Thông tin chi tiết phân khu sản xuất", response);
    }

    @Operation(summary = "UC-16: Cập nhật thông tin phân khu (Zone)")
    @PutMapping("/zones/{zoneId}")
    public ApiResp<ZoneResp> updateZone(
            @PathVariable Integer zoneId,
            @Valid @RequestBody UpdateZoneReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        ZoneResp response = zoneService.updateZone(currentUserId, zoneId, request);
        return ApiResp.ok("Cập nhật phân khu sản xuất thành công", response);
    }

    @Operation(summary = "UC-16: Xóa phân khu (Zone)")
    @DeleteMapping("/zones/{zoneId}")
    public ApiResp<Void> deleteZone(@PathVariable Integer zoneId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        zoneService.deleteZone(currentUserId, zoneId);
        return ApiResp.ok("Xóa phân khu sản xuất thành công", null);
    }

    @Operation(summary = "UC-16.1: Xem danh sách phân khu được phân công phụ trách")
    @GetMapping({"/farms/{farmId}/zones/assigned", "/farms/{farmId}/my-assigned-zones"})
    public ApiResp<List<AssignedZoneResp>> getAssignedZones(@PathVariable Integer farmId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<AssignedZoneResp> list = zoneService.getAssignedZones(currentUserId, farmId);
        return ApiResp.ok("Danh sách phân khu được phân công phụ trách", list);
    }

    @Operation(summary = "UC-16.2: Xem thông tin chi tiết phân khu được phân công")
    @GetMapping("/zones/assigned/{zoneId}")
    public ApiResp<ZoneDetailResp> getAssignedZoneDetail(@PathVariable Integer zoneId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        ZoneDetailResp response = zoneService.getAssignedZoneDetail(currentUserId, zoneId);
        return ApiResp.ok("Thông tin chi tiết phân khu được phân công", response);
    }
}
