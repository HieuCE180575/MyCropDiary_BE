package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.risk.CreateZoneRiskReq;
import com.mycropdiary.api.dto.risk.UpdateRiskControlReq;
import com.mycropdiary.api.dto.risk.ZoneRiskResp;
import com.mycropdiary.api.service.ZoneRiskService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "UC-21: Zone Production Risk Management", description = "Đánh giá, theo dõi và kiểm soát rủi ro sản xuất phân khu (Zone Risk)")
@RestController
@RequestMapping("/api/v1")
public class ZoneRiskController {
    private final ZoneRiskService riskService;
    private final SecurityUtils securityUtils;

    public ZoneRiskController(ZoneRiskService riskService, SecurityUtils securityUtils) {
        this.riskService = riskService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Ghi nhận rủi ro sản xuất phân khu (Record Zone Production Risk)")
    @PostMapping("/zones/{zoneId}/risks")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<ZoneRiskResp> recordZoneRisk(
            @PathVariable Integer zoneId,
            @Valid @RequestBody CreateZoneRiskReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        ZoneRiskResp response = riskService.recordZoneRisk(currentUserId, zoneId, request);
        return ApiResp.ok("Ghi nhận rủi ro phân khu thành công", response);
    }

    @Operation(summary = "Xem lịch sử rủi ro của phân khu (View Zone Risk History)")
    @GetMapping("/zones/{zoneId}/risks")
    public ApiResp<List<ZoneRiskResp>> getZoneRiskHistory(@PathVariable Integer zoneId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<ZoneRiskResp> history = riskService.getZoneRiskHistory(currentUserId, zoneId);
        return ApiResp.ok("Lịch sử rủi ro phân khu", history);
    }

    @Operation(summary = "Xem lịch sử rủi ro toàn trang trại (Farm Risk History)")
    @GetMapping("/farms/{farmId}/risks")
    public ApiResp<List<ZoneRiskResp>> getFarmRiskHistory(@PathVariable Integer farmId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<ZoneRiskResp> history = riskService.getFarmRiskHistory(currentUserId, farmId);
        return ApiResp.ok("Lịch sử rủi ro toàn trang trại", history);
    }

    @Operation(summary = "Xem chi tiết rủi ro (View Zone Risk Details)")
    @GetMapping("/risks/{riskId}")
    public ApiResp<ZoneRiskResp> getZoneRiskDetails(@PathVariable Integer riskId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        ZoneRiskResp response = riskService.getZoneRiskDetails(currentUserId, riskId);
        return ApiResp.ok("Chi tiết rủi ro phân khu", response);
    }

    @Operation(summary = "Cập nhật biện pháp kiểm soát rủi ro (Update Risk Control Measures)")
    @PutMapping("/risks/{riskId}")
    public ApiResp<ZoneRiskResp> updateRiskControlMeasures(
            @PathVariable Integer riskId,
            @Valid @RequestBody UpdateRiskControlReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        ZoneRiskResp response = riskService.updateRiskControlMeasures(currentUserId, riskId, request);
        return ApiResp.ok("Cập nhật biện pháp kiểm soát rủi ro thành công", response);
    }

    @Operation(summary = "Xóa bản ghi rủi ro")
    @DeleteMapping("/risks/{riskId}")
    public ApiResp<Void> deleteZoneRisk(@PathVariable Integer riskId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        riskService.deleteZoneRisk(currentUserId, riskId);
        return ApiResp.ok("Đã xóa bản ghi rủi ro", null);
    }
}

