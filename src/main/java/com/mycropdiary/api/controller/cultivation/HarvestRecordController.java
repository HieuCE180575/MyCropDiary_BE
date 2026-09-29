package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.cultivation.CreateHarvestRecordRequest;
import com.mycropdiary.api.dto.cultivation.HarvestRecordResponse;
import com.mycropdiary.api.service.cultivation.HarvestRecordService;
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
 * Controller quản lý REST API cho Bản ghi thu hoạch (UC-26).
 */
@RestController
@RequestMapping("/api/v1/harvest-records")
@Tag(name = "06. Thu hoạch (Harvest Records)", description = "APIs ghi nhận và xem lịch sử thu hoạch nông sản")
@SecurityRequirement(name = "BearerAuthentication")
public class HarvestRecordController {

    private final HarvestRecordService harvestRecordService;
    private final SecurityUtils securityUtils;

    public HarvestRecordController(HarvestRecordService harvestRecordService, SecurityUtils securityUtils) {
        this.harvestRecordService = harvestRecordService;
        this.securityUtils = securityUtils;
    }

    /**
     * Ghi nhận bản ghi thu hoạch cho một mùa vụ (UC-26).
     */
    @Operation(summary = "Tạo mới bản ghi thu hoạch", description = "Ghi nhận sản lượng thu hoạch cho một mùa vụ canh tác.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<HarvestRecordResponse> createHarvestRecord(@Valid @RequestBody CreateHarvestRecordRequest request) {
        Long currentUserId = securityUtils.getCurrentUserId();
        HarvestRecordResponse response = harvestRecordService.createHarvestRecord(currentUserId, request);
        return ApiResponse.ok("Ghi nhận thu hoạch thành công.", response);
    }

    /**
     * Xem danh sách lịch sử thu hoạch theo Mùa vụ (Có phân trang).
     */
    @Operation(summary = "Xem lịch sử thu hoạch theo mùa vụ", description = "Lấy danh sách các bản ghi thu hoạch của mùa vụ canh tác (có phân trang).")
    @GetMapping
    public ApiResponse<PageResponse<HarvestRecordResponse>> getHarvestRecordsBySeason(
            @RequestParam("cropSeasonId") Long cropSeasonId,
            @PageableDefault(size = 20) Pageable pageable) {
        Long currentUserId = securityUtils.getCurrentUserId();
        PageResponse<HarvestRecordResponse> response = harvestRecordService.getHarvestRecordsBySeason(currentUserId, cropSeasonId, pageable);
        return ApiResponse.ok("Lấy danh sách thu hoạch thành công.", response);
    }

    /**
     * Xem chi tiết một bản ghi thu hoạch.
     */
    @Operation(summary = "Xem chi tiết bản ghi thu hoạch", description = "Lấy thông tin chi tiết bản ghi thu hoạch theo ID.")
    @GetMapping("/{harvestRecordId}")
    public ApiResponse<HarvestRecordResponse> getHarvestRecordById(@PathVariable("harvestRecordId") Long harvestRecordId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        HarvestRecordResponse response = harvestRecordService.getHarvestRecordById(currentUserId, harvestRecordId);
        return ApiResponse.ok("Lấy chi tiết bản ghi thu hoạch thành công.", response);
    }
}
