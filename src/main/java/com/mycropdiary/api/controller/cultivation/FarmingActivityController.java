package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateFarmActivityReqDTO;
import com.mycropdiary.api.dto.cultivation.FarmingActivityRespDTO;
import com.mycropdiary.api.dto.cultivation.FarmingActivitySummaryRespDTO;
import com.mycropdiary.api.dto.cultivation.UpdateFarmingActivityReqDTO;
import com.mycropdiary.api.service.cultivation.FarmingActivityService;
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
 * Controller quản lý REST API cho Nhật ký canh tác (UC-24).
 */
@RestController
@RequestMapping("/api/v1/farming-activities")
@Tag(name = "04. Nhật ký canh tác (Farming Activities)", description = "APIs tạo và xem lịch sử nhật ký canh tác nông nghiệp")
@SecurityRequirement(name = "BearerAuthentication")
public class FarmingActivityController {

    private final FarmingActivityService farmingActivityService;
    private final SecurityUtils securityUtils;

    public FarmingActivityController(FarmingActivityService farmingActivityService, SecurityUtils securityUtils) {
        this.farmingActivityService = farmingActivityService;
        this.securityUtils = securityUtils;
    }

    /**
     * Tạo mới nhật ký canh tác.
     * Người ghi nhật ký tự động được xác định từ tài khoản người dùng đăng nhập.
     *
     * @param request Thông tin nhật ký canh tác
     * @return Thông tin nhật ký vừa tạo
     */
    @Operation(summary = "Tạo mới nhật ký canh tác", description = "Tạo mới nhật ký canh tác cho một mùa vụ. Người ghi nhật ký tự động được xác định từ tài khoản đã đăng nhập.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<FarmingActivityRespDTO> createActivity(@Valid @RequestBody CreateFarmActivityReqDTO request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        FarmingActivityRespDTO response = farmingActivityService.createActivity(currentUserId, request);
        return ApiResp.ok("Tạo nhật ký canh tác thành công.", response);
    }

    /**
     * Cập nhật nhật ký canh tác (UC-24.3).
     *
     * @param activityId ID nhật ký canh tác cần sửa
     * @param request Thông tin cập nhật nhật ký
     * @return Thông tin nhật ký sau khi cập nhật
     */
    @Operation(summary = "Cập nhật nhật ký canh tác", description = "Chỉnh sửa thông tin nhật ký canh tác đã tạo (UC-24.3).")
    @PutMapping("/{activityId}")
    public ApiResp<FarmingActivityRespDTO> updateActivity(
            @PathVariable Integer activityId,
            @Valid @RequestBody UpdateFarmingActivityReqDTO request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        FarmingActivityRespDTO response = farmingActivityService.updateActivity(currentUserId, activityId, request);
        return ApiResp.ok("Cập nhật nhật ký canh tác thành công.", response);
    }

    /**
     * Lấy danh sách nhật ký canh tác theo Mùa vụ (Có phân trang, tìm kiếm và lọc).
     *
     * @param cropSeasonId ID của mùa vụ canh tác
     * @param keyword Từ khóa tìm kiếm theo tên hoặc mô tả hoạt động
     * @param activityType Loại hoạt động canh tác
     * @param fromDate Thời gian bắt đầu
     * @param toDate Thời gian kết thúc
     * @param pageable Cấu hình phân trang
     * @return Danh sách lịch sử nhật ký canh tác dạng phân trang
     */
    @Operation(summary = "Xem lịch sử nhật ký canh tác theo mùa vụ", description = "Lấy danh sách nhật ký canh tác của mùa vụ (Có phân trang, tìm kiếm theo từ khóa và lọc nâng cao).")
    @GetMapping
    public ApiResp<PageResp<FarmingActivitySummaryRespDTO>> getActivitiesByCropSeason(
            @RequestParam Integer cropSeasonId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 20) Pageable pageable) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        PageResp<FarmingActivitySummaryRespDTO> response = farmingActivityService.searchAndFilterActivities(
                currentUserId, cropSeasonId, keyword, activityType, fromDate, toDate, pageable
        );
        return ApiResp.ok("Danh sách lịch sử nhật ký canh tác.", response);
    }

    /**
     * Xem chi tiết một nhật ký canh tác theo ID.
     *
     * @param activityId ID nhật ký canh tác
     * @return Thông tin chi tiết nhật ký canh tác kèm danh sách công nhân thực hiện
     */
    @Operation(summary = "Xem chi tiết nhật ký canh tác", description = "Lấy chi tiết một nhật ký canh tác theo ID bao gồm danh sách công nhân trực tiếp thực hiện.")
    @GetMapping("/{activityId}")
    public ApiResp<FarmingActivityRespDTO> getActivityDetail(@PathVariable Integer activityId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        FarmingActivityRespDTO response = farmingActivityService.getActivityDetail(currentUserId, activityId);
        return ApiResp.ok("Chi tiết nhật ký canh tác.", response);
    }
}

