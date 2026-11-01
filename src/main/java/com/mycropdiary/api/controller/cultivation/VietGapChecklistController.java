package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.vietgap.ChecklistResultItemResp;
import com.mycropdiary.api.dto.vietgap.ChecklistRunResp;
import com.mycropdiary.api.dto.vietgap.RunChecklistReq;
import com.mycropdiary.api.service.cultivation.VietGapChecklistService;
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
@Tag(name = "08.1. Danh mục kiểm tra VietGAP (VietGAP Evidence Checklist)", description = "APIs chạy danh mục kiểm tra, bằng chứng hiện trường và lịch sử thực thi theo quy chuẩn VietGAP")
@SecurityRequirement(name = "BearerAuthentication")
public class VietGapChecklistController {

    private final VietGapChecklistService checklistService;
    private final SecurityUtils securityUtils;

    public VietGapChecklistController(VietGapChecklistService checklistService, SecurityUtils securityUtils) {
        this.checklistService = checklistService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Run VietGAP Evidence Checklist", description = "Kích hoạt và thực thi lượt kiểm tra danh mục bằng chứng VietGAP theo mùa vụ/lô đất.")
    @PostMapping("/farms/{farmId}/checklist-runs")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<ChecklistRunResp> runChecklist(
            @PathVariable Integer farmId,
            @Valid @RequestBody RunChecklistReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        ChecklistRunResp response = checklistService.runChecklist(currentUserId, farmId, request);
        return ApiResp.ok("Thực thi danh mục kiểm tra VietGAP thành công.", response);
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

