package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateHarvestRecordReqDTO;
import com.mycropdiary.api.dto.cultivation.HarvestRecordRespDTO;
import com.mycropdiary.api.dto.cultivation.UpdateHarvestRecordReqDTO;
import com.mycropdiary.api.service.cultivation.HarvestRecordService;
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
    public ApiResp<HarvestRecordRespDTO> createHarvestRecord(@Valid @RequestBody CreateHarvestRecordReqDTO request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        HarvestRecordRespDTO response = harvestRecordService.createHarvestRecord(currentUserId, request);
        return ApiResp.ok("Ghi nhận thu hoạch thành công.", response);
    }

    /**
     * Cập nhật bản ghi thu hoạch (UC-26.3).
     */
    @Operation(summary = "Cập nhật bản ghi thu hoạch", description = "Chỉnh sửa sản lượng hoặc thông tin bản ghi thu hoạch đã tạo (UC-26.3).")
    @PutMapping("/{harvestRecordId}")
    public ApiResp<HarvestRecordRespDTO> updateHarvestRecord(
            @PathVariable("harvestRecordId") Integer harvestRecordId,
            @Valid @RequestBody UpdateHarvestRecordReqDTO request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        HarvestRecordRespDTO response = harvestRecordService.updateHarvestRecord(currentUserId, harvestRecordId, request);
        return ApiResp.ok("Cập nhật bản ghi thu hoạch thành công.", response);
    }

    /**
     * Xem danh sách lịch sử thu hoạch theo Mùa vụ (Có phân trang, tìm kiếm và lọc).
     */
    @Operation(summary = "Xem lịch sử thu hoạch theo mùa vụ", description = "Lấy danh sách các bản ghi thu hoạch của mùa vụ canh tác (có phân trang, tìm kiếm theo từ khóa và lọc theo chất lượng, khoảng ngày).")
    @GetMapping
    public ApiResp<PageResp<HarvestRecordRespDTO>> getHarvestRecords(
            @RequestParam("cropSeasonId") Integer cropSeasonId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String qualityGrade,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 20) Pageable pageable) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        PageResp<HarvestRecordRespDTO> response = harvestRecordService.searchAndFilterHarvestRecords(
                currentUserId, cropSeasonId, keyword, qualityGrade, fromDate, toDate, pageable);
        return ApiResp.ok("Lấy danh sách thu hoạch thành công.", response);
    }

    /**
     * Xem chi tiết một bản ghi thu hoạch.
     */

    /**
     * Tìm kiếm và lọc lịch sử thu hoạch toàn trang trại (Search Harvest Records & Filter Harvest Records).
     */
    @Operation(summary = "Tìm kiếm và lọc lịch sử thu hoạch theo trang trại", description = "Tìm kiếm theo từ khóa và lọc theo mùa vụ, lô đất, chất lượng hoặc khoảng thời gian trên toàn trang trại.")
    @GetMapping("/farms/{farmId}")
    public ApiResp<PageResp<HarvestRecordRespDTO>> getFarmHarvestRecords(
            @PathVariable Integer farmId,
            @RequestParam(required = false) Integer cropSeasonId,
            @RequestParam(required = false) Integer blockId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String qualityGrade,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 20) Pageable pageable) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        PageResp<HarvestRecordRespDTO> response = harvestRecordService.searchFarmHarvestRecords(
                currentUserId, farmId, cropSeasonId, blockId, keyword, qualityGrade, fromDate, toDate, pageable);
        return ApiResp.ok("Lấy danh sách thu hoạch trang trại thành công.", response);
    }

    @Operation(summary = "Xem chi tiết bản ghi thu hoạch", description = "Lấy thông tin chi tiết bản ghi thu hoạch theo ID.")
    @GetMapping("/{harvestRecordId}")
    public ApiResp<HarvestRecordRespDTO> getHarvestRecordById(@PathVariable("harvestRecordId") Integer harvestRecordId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        HarvestRecordRespDTO response = harvestRecordService.getHarvestRecordById(currentUserId, harvestRecordId);
        return ApiResp.ok("Lấy chi tiết bản ghi thu hoạch thành công.", response);
    }
}
