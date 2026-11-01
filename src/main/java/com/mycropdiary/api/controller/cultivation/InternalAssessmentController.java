package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.vietgap.ConductInternalAssessmentReq;
import com.mycropdiary.api.dto.vietgap.InternalAssessmentResp;
import com.mycropdiary.api.service.cultivation.InternalAssessmentService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "08.2. Đánh giá nội bộ VietGAP (Internal VietGAP Assessment)", description = "APIs tiến hành thẩm định nội bộ, chấm điểm tiêu chí VietGAP và lưu trữ lịch sử đánh giá")
@SecurityRequirement(name = "BearerAuthentication")
public class InternalAssessmentController {

    private final InternalAssessmentService assessmentService;
    private final SecurityUtils securityUtils;

    public InternalAssessmentController(InternalAssessmentService assessmentService, SecurityUtils securityUtils) {
        this.assessmentService = assessmentService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Conduct Internal VietGAP Assessment", description = "Tiến hành một đợt đánh giá nội bộ toàn diện theo các tiêu chí chuẩn VietGAP, chấm điểm và đính kèm bằng chứng thẩm định.")
    @PostMapping("/farms/{farmId}/internal-assessments")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<InternalAssessmentResp> conductInternalAssessment(
            @PathVariable Integer farmId,
            @Valid @RequestBody ConductInternalAssessmentReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        InternalAssessmentResp response = assessmentService.conductInternalAssessment(currentUserId, farmId, request);
        return ApiResp.ok("Hoàn thành đợt đánh giá nội bộ VietGAP thành công.", response);
    }

    @Operation(summary = "View Internal Assessment History", description = "Xem lịch sử các đợt đánh giá nội bộ của trang trại (hỗ trợ lọc theo mùa vụ hoặc phân khu).")
    @GetMapping("/farms/{farmId}/internal-assessments")
    public ApiResp<List<InternalAssessmentResp>> getInternalAssessmentHistory(
            @PathVariable Integer farmId,
            @RequestParam(required = false) Integer cropSeasonId,
            @RequestParam(required = false) Integer zoneId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        List<InternalAssessmentResp> history = assessmentService.getInternalAssessmentHistory(currentUserId, farmId, cropSeasonId, zoneId);
        return ApiResp.ok("Lịch sử đánh giá nội bộ VietGAP.", history);
    }

    @Operation(summary = "View Internal Assessment Details", description = "Xem chi tiết một đợt đánh giá nội bộ, danh sách điểm số từng tiêu chí, tỷ lệ tuân thủ và bằng chứng.")
    @GetMapping("/internal-assessments/{assessmentId}")
    public ApiResp<InternalAssessmentResp> getInternalAssessmentDetails(@PathVariable Integer assessmentId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        InternalAssessmentResp response = assessmentService.getInternalAssessmentDetails(currentUserId, assessmentId);
        return ApiResp.ok("Chi tiết đợt đánh giá nội bộ VietGAP.", response);
    }
}

