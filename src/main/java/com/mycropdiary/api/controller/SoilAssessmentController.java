package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.soil.CreateSoilAssessmentReq;
import com.mycropdiary.api.dto.soil.SoilAssessmentResp;
import com.mycropdiary.api.service.SoilAssessmentService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "UC-20: Soil Assessment Management", description = "Đánh giá, giám sát và lưu trữ lịch sử thổ nhưỡng lô đất (Soil Assessment)")
@RestController
@RequestMapping("/api/v1")
public class SoilAssessmentController {
    private final SoilAssessmentService soilService;
    private final SecurityUtils securityUtils;

    public SoilAssessmentController(SoilAssessmentService soilService, SecurityUtils securityUtils) {
        this.soilService = soilService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Tạo mới đánh giá thổ nhưỡng lô đất (Create Soil Assessment)")
    @PostMapping("/blocks/{blockId}/soil-assessments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<SoilAssessmentResp> createSoilAssessment(
            @PathVariable Integer blockId,
            @Valid @RequestBody CreateSoilAssessmentReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        SoilAssessmentResp response = soilService.createSoilAssessment(currentUserId, blockId, request);
        return ApiResp.ok("Tạo đánh giá thổ nhưỡng thành công", response);
    }

    @Operation(summary = "Xem lịch sử đánh giá thổ nhưỡng của lô đất (View Soil Assessment History)")
    @GetMapping("/blocks/{blockId}/soil-assessments")
    public ApiResp<List<SoilAssessmentResp>> getSoilAssessmentHistory(@PathVariable Integer blockId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<SoilAssessmentResp> history = soilService.getSoilAssessmentHistory(currentUserId, blockId);
        return ApiResp.ok("Lịch sử đánh giá thổ nhưỡng lô đất", history);
    }

    @Operation(summary = "Xem lịch sử đánh giá thổ nhưỡng toàn trang trại (Farm Soil Assessment History)")
    @GetMapping("/farms/{farmId}/soil-assessments")
    public ApiResp<List<SoilAssessmentResp>> getFarmSoilAssessmentHistory(@PathVariable Integer farmId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<SoilAssessmentResp> history = soilService.getFarmSoilAssessmentHistory(currentUserId, farmId);
        return ApiResp.ok("Lịch sử đánh giá thổ nhưỡng toàn trang trại", history);
    }

    @Operation(summary = "Xem chi tiết đánh giá thổ nhưỡng (Soil Assessment Details)")
    @GetMapping("/soil-assessments/{assessmentId}")
    public ApiResp<SoilAssessmentResp> getSoilAssessmentDetails(@PathVariable Integer assessmentId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        SoilAssessmentResp response = soilService.getSoilAssessmentDetails(currentUserId, assessmentId);
        return ApiResp.ok("Chi tiết đánh giá thổ nhưỡng", response);
    }

    @Operation(summary = "Xóa phiếu đánh giá thổ nhưỡng")
    @DeleteMapping("/soil-assessments/{assessmentId}")
    public ApiResp<Void> deleteSoilAssessment(@PathVariable Integer assessmentId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        soilService.deleteSoilAssessment(currentUserId, assessmentId);
        return ApiResp.ok("Đã xóa phiếu đánh giá thổ nhưỡng", null);
    }
}

