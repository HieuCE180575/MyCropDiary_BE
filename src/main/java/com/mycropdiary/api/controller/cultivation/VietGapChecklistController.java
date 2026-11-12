package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.vietgap.*;
import com.mycropdiary.api.service.cultivation.VietGapChecklistService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "08.1. Danh mục kiểm tra VietGAP (VietGAP Evidence Checklist)", description = "APIs chạy danh mục kiểm tra, bằng chứng hiện trường, tự động đối soát và xuất báo cáo in ấn theo chuẩn VietGAP")
@SecurityRequirement(name = "BearerAuthentication")
public class VietGapChecklistController {

    private final VietGapChecklistService checklistService;
    private final SecurityUtils securityUtils;

    public VietGapChecklistController(VietGapChecklistService checklistService, SecurityUtils securityUtils) {
        this.checklistService = checklistService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Init VietGAP Comprehensive Audit Run", description = "Khởi tạo đợt tự đánh giá VietGAP toàn diện (33 tiêu chí Master Rule) và tự động quét đối soát dữ liệu.")
    @PostMapping("/farms/{farmId}/checklist-runs/init")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<ChecklistRunResp> initAuditRun(
            @PathVariable Integer farmId,
            @Valid @RequestBody InitVietGapAuditRunReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        ChecklistRunResp response = checklistService.initVietGapAuditRun(currentUserId, farmId, request);
        return ApiResp.ok("Khởi tạo đợt tự đánh giá VietGAP toàn diện thành công.", response);
    }

    @Operation(summary = "Run VietGAP Evidence Checklist (Legacy)", description = "Kích hoạt và thực thi lượt kiểm tra danh mục bằng chứng VietGAP theo mùa vụ/lô đất.")
    @PostMapping("/farms/{farmId}/checklist-runs")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<ChecklistRunResp> runChecklist(
            @PathVariable Integer farmId,
            @Valid @RequestBody RunChecklistReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        ChecklistRunResp response = checklistService.runChecklist(currentUserId, farmId, request);
        return ApiResp.ok("Thực thi danh mục kiểm tra VietGAP thành công.", response);
    }

    @Operation(summary = "Update Checklist Item and Evidence", description = "Cập nhật trạng thái (Pass/Fail/NA), ghi chú và ảnh minh chứng hiện trường cho 1 tiêu chí kiểm tra.")
    @PutMapping("/farms/{farmId}/checklist-runs/{runId}/items/{resultId}")
    public ApiResp<ChecklistResultItemResp> updateChecklistItem(
            @PathVariable Integer farmId,
            @PathVariable Integer runId,
            @PathVariable Integer resultId,
            @Valid @RequestBody UpdateChecklistItemReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        ChecklistResultItemResp response = checklistService.updateChecklistItem(currentUserId, farmId, runId, resultId, request);
        return ApiResp.ok("Cập nhật mục kiểm tra hiện trường thành công.", response);
    }

    @Operation(summary = "Complete & Lock Checklist Run", description = "Chốt biên bản kiểm tra, tính % tuân thủ và khóa bất biến (Snapshot) không cho sửa đổi.")
    @PostMapping("/farms/{farmId}/checklist-runs/{runId}/complete")
    public ApiResp<ChecklistRunResp> completeChecklistRun(
            @PathVariable Integer farmId,
            @PathVariable Integer runId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        ChecklistRunResp response = checklistService.completeChecklistRun(currentUserId, farmId, runId);
        return ApiResp.ok("Hoàn tất và khóa biên bản tự đánh giá VietGAP thành công.", response);
    }

    @Operation(summary = "Auto-Check & Re-evaluate Run", description = "Chạy lại thuật toán quét tự động đối soát từ nhật ký canh tác, đất, nước, thu hoạch.")
    @PostMapping("/farms/{farmId}/checklist-runs/{runId}/auto-check")
    public ApiResp<ChecklistRunResp> autoCheckRun(
            @PathVariable Integer farmId,
            @PathVariable Integer runId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        ChecklistRunResp response = checklistService.autoCheckRun(currentUserId, farmId, runId);
        return ApiResp.ok("Tự động đối soát lại dữ liệu thành công.", response);
    }

    @Operation(summary = "Export VietGAP Audit Report PDF", description = "Xuất và tải về Báo cáo tự đánh giá nội bộ VietGAP khổ A4 in ấn chuẩn có chỗ ký tên đóng dấu.")
    @GetMapping("/farms/{farmId}/checklist-runs/{runId}/export-pdf")
    public ResponseEntity<byte[]> exportAuditPdf(
            @PathVariable Integer farmId,
            @PathVariable Integer runId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        byte[] pdfBytes = checklistService.exportAuditReportPdf(currentUserId, farmId, runId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Bao_cao_tu_danh_gia_VietGAP_Run_" + runId + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @Operation(summary = "View Checklist Result", description = "Xem kết quả tổng hợp của một lượt thực thi danh mục kiểm tra.")
    @GetMapping("/checklist-runs/{runId}")
    public ApiResp<ChecklistRunResp> getChecklistResult(@PathVariable Integer runId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        ChecklistRunResp response = checklistService.getChecklistResult(currentUserId, runId);
        return ApiResp.ok("Kết quả lượt kiểm tra VietGAP.", response);
    }

    @Operation(summary = "View Checklist Item Details and Missing Evidence", description = "Xem chi tiết từng mục kiểm tra, kết quả đạt/không đạt và phát hiện các mục thiếu ảnh bằng chứng.")
    @GetMapping("/checklist-runs/{runId}/items")
    public ApiResp<List<ChecklistResultItemResp>> getChecklistItemDetails(@PathVariable Integer runId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        List<ChecklistResultItemResp> items = checklistService.getChecklistItemDetails(currentUserId, runId);
        return ApiResp.ok("Chi tiết các mục kiểm tra và bằng chứng hiện trường.", items);
    }

    @Operation(summary = "View Checklist Run History", description = "Xem lịch sử các lượt thực thi danh mục kiểm tra của trang trại (có thể lọc theo mùa vụ và trạng thái runStatus).")
    @GetMapping("/farms/{farmId}/checklist-runs")
    public ApiResp<List<ChecklistRunResp>> getChecklistRunHistory(
            @PathVariable Integer farmId,
            @RequestParam(required = false) Integer cropSeasonId,
            @RequestParam(required = false) String runStatus) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        List<ChecklistRunResp> history = checklistService.getChecklistRunHistory(currentUserId, farmId, cropSeasonId, runStatus);
        return ApiResp.ok("Lịch sử các lượt kiểm tra VietGAP.", history);
    }
}
