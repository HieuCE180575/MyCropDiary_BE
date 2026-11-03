package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.water.CreateWaterAssessmentReq;
import com.mycropdiary.api.dto.water.UpdateWaterAssessmentReq;
import com.mycropdiary.api.dto.water.WaterAssessmentResp;
import com.mycropdiary.api.service.WaterAssessmentService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "11. Đánh giá chất lượng nước tưới tiêu (Water Assessment Management)", description = "Đánh giá, giám sát và lưu trữ lịch sử chất lượng nguồn nước tưới tiêu (Water History)")
@RestController
@RequestMapping("/api/v1")
public class WaterAssessmentController {

    private final WaterAssessmentService waterService;
    private final SecurityUtils securityUtils;

    public WaterAssessmentController(WaterAssessmentService waterService, SecurityUtils securityUtils) {
        this.waterService = waterService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Tạo mới đánh giá chất lượng nguồn nước theo Block (Record Water Assessment)")
    @PostMapping("/blocks/{blockId}/water-assessments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<WaterAssessmentResp> createWaterAssessment(
            @PathVariable Integer blockId,
            @Valid @RequestBody CreateWaterAssessmentReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        WaterAssessmentResp response = waterService.createWaterAssessment(currentUserId, blockId, request);
        return ApiResp.ok("Tạo phiếu đánh giá nguồn nước thành công", response);
    }

    @Operation(summary = "Xem lịch sử đánh giá nước của Block (View Water History)")
    @GetMapping("/blocks/{blockId}/water-assessments")
    public ApiResp<List<WaterAssessmentResp>> getWaterAssessmentHistory(@PathVariable Integer blockId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<WaterAssessmentResp> history = waterService.getWaterAssessmentHistory(currentUserId, blockId);
        return ApiResp.ok("Lịch sử đánh giá chất lượng nước lô đất", history);
    }

    @Operation(summary = "Xem lịch sử đánh giá nguồn nước toàn trang trại (View Water History)")
    @GetMapping("/farms/{farmId}/water-assessments")
    public ApiResp<List<WaterAssessmentResp>> getFarmWaterAssessmentHistory(@PathVariable Integer farmId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<WaterAssessmentResp> history = waterService.getFarmWaterAssessmentHistory(currentUserId, farmId);
        return ApiResp.ok("Lịch sử đánh giá nguồn nước toàn trang trại", history);
    }

    @Operation(summary = "Xem chi tiết đánh giá nguồn nước (View Water Assessment Details)")
    @GetMapping("/water-assessments/{assessmentId}")
    public ApiResp<WaterAssessmentResp> getWaterAssessmentDetails(@PathVariable Integer assessmentId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        WaterAssessmentResp response = waterService.getWaterAssessmentDetails(currentUserId, assessmentId);
        return ApiResp.ok("Chi tiết đánh giá nguồn nước", response);
    }

    @Operation(summary = "Cập nhật phiếu đánh giá nguồn nước (Update Water Assessment)")
    @PutMapping("/water-assessments/{assessmentId}")
    public ApiResp<WaterAssessmentResp> updateWaterAssessment(
            @PathVariable Integer assessmentId,
            @Valid @RequestBody UpdateWaterAssessmentReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        WaterAssessmentResp response = waterService.updateWaterAssessment(currentUserId, assessmentId, request);
        return ApiResp.ok("Cập nhật đánh giá nguồn nước thành công", response);
    }

    @Operation(summary = "Xóa phiếu đánh giá nguồn nước đủ điều kiện (Delete Eligible Water Assessment)")
    @DeleteMapping("/water-assessments/{assessmentId}")
    public ApiResp<Void> deleteWaterAssessment(@PathVariable Integer assessmentId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        waterService.deleteWaterAssessment(currentUserId, assessmentId);
        return ApiResp.ok("Đã xóa phiếu đánh giá nguồn nước thành công", null);
    }
}

