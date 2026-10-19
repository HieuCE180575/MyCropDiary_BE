package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateFarmingActivityReq;
import com.mycropdiary.api.dto.cultivation.FarmingActivityResp;
import com.mycropdiary.api.dto.cultivation.FarmingActivitySummaryResp;
import com.mycropdiary.api.service.cultivation.FarmingActivityService;
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
    public ApiResp<FarmingActivityResp> createActivity(@Valid @RequestBody CreateFarmingActivityReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        FarmingActivityResp response = farmingActivityService.createActivity(currentUserId, request);
        return ApiResp.ok("Tạo nhật ký canh tác thành công.", response);
    }

    /**
     * Lấy danh sách nhật ký canh tác theo Mùa vụ (Có phân trang).
     *
     * @param cropSeasonId ID của mùa vụ canh tác
     * @param pageable Cấu hình phân trang
     * @return Danh sách lịch sử nhật ký canh tác dạng phân trang
     */
    @Operation(summary = "Xem lịch sử nhật ký canh tác theo mùa vụ", description = "Lấy danh sách nhật ký canh tác của mùa vụ (Có phân trang).")
    @GetMapping
    public ApiResp<PageResp<FarmingActivitySummaryResp>> getActivitiesByCropSeason(
            @RequestParam Integer cropSeasonId,
            @PageableDefault(size = 20) Pageable pageable) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        PageResp<FarmingActivitySummaryResp> response = farmingActivityService.getActivitiesByCropSeason(currentUserId, cropSeasonId, pageable);
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
    public ApiResp<FarmingActivityResp> getActivityDetail(@PathVariable Integer activityId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        FarmingActivityResp response = farmingActivityService.getActivityDetail(currentUserId, activityId);
        return ApiResp.ok("Chi tiết nhật ký canh tác.", response);
    }
}
