package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.vietgap.CreateVietGapCriterionReq;
import com.mycropdiary.api.dto.vietgap.UpdateVietGapCriterionReq;
import com.mycropdiary.api.dto.vietgap.VietGapCriterionResp;
import com.mycropdiary.api.service.cultivation.VietGapCriterionService;
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
@Tag(name = "08. Tiêu chuẩn VietGAP (VietGAP Criteria)", description = "APIs quản lý tiêu chuẩn đánh giá nội bộ theo chuẩn TCVN 11892-1:2017 và phương pháp đánh giá")
@SecurityRequirement(name = "BearerAuthentication")
public class VietGapCriterionController {

    private final VietGapCriterionService criterionService;
    private final SecurityUtils securityUtils;

    public VietGapCriterionController(VietGapCriterionService criterionService, SecurityUtils securityUtils) {
        this.criterionService = criterionService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "View VietGAP Criterion List", description = "Xem danh sách các tiêu chuẩn đánh giá VietGAP của trang trại (hỗ trợ lọc trạng thái kích hoạt và tìm kiếm từ khóa).")
    @GetMapping("/farms/{farmId}/vietgap-criteria")
    public ApiResp<List<VietGapCriterionResp>> getCriterionList(
            @PathVariable Integer farmId,
            @RequestParam(required = false) Boolean isActiveOnly,
            @RequestParam(required = false) String keyword) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        List<VietGapCriterionResp> list = criterionService.getCriterionList(currentUserId, farmId, isActiveOnly, keyword);
        return ApiResp.ok("Lấy danh sách tiêu chuẩn VietGAP thành công.", list);
    }

    @Operation(summary = "View VietGAP Criterion Details", description = "Xem chi tiết tiêu chuẩn, mô tả chi tiết và phương pháp đánh giá.")
    @GetMapping("/vietgap-criteria/{criterionId}")
    public ApiResp<VietGapCriterionResp> getCriterionDetails(@PathVariable Integer criterionId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        VietGapCriterionResp details = criterionService.getCriterionDetails(currentUserId, criterionId);
        return ApiResp.ok("Chi tiết tiêu chuẩn VietGAP.", details);
    }

    @Operation(summary = "Create VietGAP Criterion from TCVN 11892-1:2017", description = "Tạo mới một tiêu chuẩn đánh giá theo quy chuẩn VietGAP TCVN 11892-1:2017.")
    @PostMapping("/farms/{farmId}/vietgap-criteria")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<VietGapCriterionResp> createCriterion(
            @PathVariable Integer farmId,
            @Valid @RequestBody CreateVietGapCriterionReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        VietGapCriterionResp response = criterionService.createCriterion(currentUserId, farmId, request);
        return ApiResp.ok("Tạo tiêu chuẩn VietGAP thành công.", response);
    }

    @Operation(summary = "Import Full TCVN 11892-1:2017 Standard Criteria", description = "Tự động khởi tạo bộ 10 tiêu chuẩn đầy đủ theo chuẩn quốc gia VietGAP TCVN 11892-1:2017 cho trang trại.")
    @PostMapping("/farms/{farmId}/vietgap-criteria/import-tcvn")
    public ApiResp<List<VietGapCriterionResp>> importTcvnCriteria(@PathVariable Integer farmId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        List<VietGapCriterionResp> response = criterionService.importTcvnStandardCriteria(currentUserId, farmId);
        return ApiResp.ok("Đã khởi tạo bộ tiêu chuẩn TCVN 11892-1:2017 thành công.", response);
    }

    @Operation(summary = "Update VietGAP Criterion and Evaluation Method", description = "Cập nhật tên tiêu chuẩn, mô tả, thang điểm, phương pháp đánh giá và trạng thái.")
    @PutMapping("/vietgap-criteria/{criterionId}")
    public ApiResp<VietGapCriterionResp> updateCriterion(
            @PathVariable Integer criterionId,
            @Valid @RequestBody UpdateVietGapCriterionReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        VietGapCriterionResp response = criterionService.updateCriterion(currentUserId, criterionId, request);
        return ApiResp.ok("Cập nhật tiêu chuẩn và phương pháp đánh giá thành công.", response);
    }

    @Operation(summary = "Delete VietGAP Criterion", description = "Xóa tiêu chuẩn đánh giá khỏi trang trại.")
    @DeleteMapping("/vietgap-criteria/{criterionId}")
    public ApiResp<Void> deleteCriterion(@PathVariable Integer criterionId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        criterionService.deleteCriterion(currentUserId, criterionId);
        return ApiResp.ok("Đã xóa tiêu chuẩn VietGAP.", null);
    }
}

