package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.report.CropSeasonReportResp;
import com.mycropdiary.api.dto.report.FarmReportResp;
import com.mycropdiary.api.service.cultivation.ReportService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "13. Báo cáo sản xuất & Tài chính (Reports & Analytics)", description = "APIs tổng hợp báo cáo sản lượng thu hoạch, hạch toán chi phí và phân tích hiệu quả mùa vụ")
@SecurityRequirement(name = "BearerAuthentication")
public class ReportController {

    private final ReportService reportService;
    private final SecurityUtils securityUtils;

    public ReportController(ReportService reportService, SecurityUtils securityUtils) {
        this.reportService = reportService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Xem báo cáo tổng hợp trang trại", description = "Lấy báo cáo tổng hợp sản lượng, chi phí, doanh thu ước tính và cơ cấu tài chính toàn trang trại.")
    @GetMapping("/farms/{farmId}/reports/summary")
    public ApiResp<FarmReportResp> getFarmReport(@PathVariable Integer farmId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        FarmReportResp response = reportService.generateFarmReport(currentUserId, farmId);
        return ApiResp.ok("Báo cáo tổng hợp trang trại.", response);
    }

    @Operation(summary = "Xem báo cáo mùa vụ canh tác", description = "Lấy báo cáo chi tiết sản lượng, chi phí và nhật ký canh tác của một mùa vụ cụ thể.")
    @GetMapping("/crop-seasons/{cropSeasonId}/report")
    public ApiResp<CropSeasonReportResp> getCropSeasonReport(@PathVariable Integer cropSeasonId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        CropSeasonReportResp response = reportService.getCropSeasonReport(currentUserId, cropSeasonId);
        return ApiResp.ok("Báo cáo mùa vụ canh tác.", response);
    }
}

