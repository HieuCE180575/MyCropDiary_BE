package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.soil.CreateSoilReqDTO;
import com.mycropdiary.api.dto.soil.SoilAssessmentRespDTO;
import com.mycropdiary.api.dto.soil.UpdateSoilReqDTO;
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
    public ApiResp<SoilAssessmentRespDTO> createSoilAssessment(
            @PathVariable Integer blockId,
            @Valid @RequestBody CreateSoilReqDTO request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        SoilAssessmentRespDTO response = soilService.createSoilAssessment(currentUserId, blockId, request);
        return ApiResp.ok("Tạo đánh giá thổ nhưỡng thành công", response);
    }

    @Operation(summary = "Xem lịch sử đánh giá thổ nhưỡng của lô đất (View Soil Assessment History)")
    @GetMapping("/blocks/{blockId}/soil-assessments")
    public ApiResp<List<SoilAssessmentRespDTO>> getSoilAssessmentHistory(@PathVariable Integer blockId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<SoilAssessmentRespDTO> history = soilService.getSoilAssessmentHistory(currentUserId, blockId);
        return ApiResp.ok("Lịch sử đánh giá thổ nhưỡng lô đất", history);
    }

    @Operation(summary = "Xem lịch sử đánh giá thổ nhưỡng toàn trang trại (Farm Soil Assessment History)")
    @GetMapping("/farms/{farmId}/soil-assessments")
    public ApiResp<List<SoilAssessmentRespDTO>> getFarmSoilAssessmentHistory(@PathVariable Integer farmId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<SoilAssessmentRespDTO> history = soilService.getFarmSoilAssessmentHistory(currentUserId, farmId);
        return ApiResp.ok("Lịch sử đánh giá thổ nhưỡng toàn trang trại", history);
    }

    @Operation(summary = "Xem chi tiết đánh giá thổ nhưỡng (View Soil Assessment Details)")
    @GetMapping("/soil-assessments/{assessmentId}")
    public ApiResp<SoilAssessmentRespDTO> getSoilAssessmentDetails(@PathVariable Integer assessmentId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        SoilAssessmentRespDTO response = soilService.getSoilAssessmentDetails(currentUserId, assessmentId);
        return ApiResp.ok("Chi tiết đánh giá thổ nhưỡng", response);
    }

    @Operation(summary = "Cập nhật phiếu đánh giá thổ nhưỡng (Update Soil Assessment)")
    @PutMapping("/soil-assessments/{assessmentId}")
    public ApiResp<SoilAssessmentRespDTO> updateSoilAssessment(
            @PathVariable Integer assessmentId,
            @Valid @RequestBody UpdateSoilReqDTO request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        SoilAssessmentRespDTO response = soilService.updateSoilAssessment(currentUserId, assessmentId, request);
        return ApiResp.ok("Cập nhật đánh giá thổ nhưỡng thành công", response);
    }

    @Operation(summary = "Xóa phiếu đánh giá thổ nhưỡng đủ điều kiện (Delete Eligible Soil Assessment)")
    @DeleteMapping("/soil-assessments/{assessmentId}")
    public ApiResp<Void> deleteSoilAssessment(@PathVariable Integer assessmentId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        soilService.deleteSoilAssessment(currentUserId, assessmentId);
        return ApiResp.ok("Đã xóa phiếu đánh giá thổ nhưỡng thành công", null);
    }
}

