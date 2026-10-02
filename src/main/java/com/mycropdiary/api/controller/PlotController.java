package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.dto.plot.CreatePlotRequest;
import com.mycropdiary.api.dto.plot.PlotResponse;
import com.mycropdiary.api.dto.plot.UpdatePlotRequest;
import com.mycropdiary.api.util.SecurityUtils;
import com.mycropdiary.api.service.PlotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PlotController {
    private final PlotService plotService;
    private final SecurityUtils securityUtils;

    public PlotController(PlotService plotService, SecurityUtils securityUtils) {
        this.plotService = plotService;
        this.securityUtils = securityUtils;
    }

    @PostMapping("/production-areas/{productionAreaId}/plots")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlotResponse> createPlot(@PathVariable Long productionAreaId,
                                                @Valid @RequestBody CreatePlotRequest request) {
        Long currentUserId = securityUtils.getRequiredCurrentUserId();
        PlotResponse response = plotService.createPlot(currentUserId, productionAreaId, request);
        return ApiResponse.ok("Tạo thửa đất thành công", response);
    }

    @GetMapping("/plots/{plotId}")
    public ApiResponse<PlotResponse> getPlotById(@PathVariable Long plotId) {
        Long currentUserId = securityUtils.getRequiredCurrentUserId();
        PlotResponse response = plotService.getPlotById(currentUserId, plotId);
        return ApiResponse.ok("Thông tin chi tiết thửa đất", response);
    }

    @PutMapping("/plots/{plotId}")
    public ApiResponse<PlotResponse> updatePlot(@PathVariable Long plotId,
                                                @Valid @RequestBody UpdatePlotRequest request) {
        Long currentUserId = securityUtils.getRequiredCurrentUserId();
        PlotResponse response = plotService.updatePlot(currentUserId, plotId, request);
        return ApiResponse.ok("Cập nhật thửa đất thành công", response);
    }

    @DeleteMapping("/plots/{plotId}")
    public ApiResponse<Void> deletePlot(@PathVariable Long plotId) {
        Long currentUserId = securityUtils.getRequiredCurrentUserId();
        plotService.deletePlot(currentUserId, plotId);
        return ApiResponse.ok("Đã ngừng hoạt động thửa đất", null);
    }

    @GetMapping("/plots")
    public ApiResponse<List<PlotResponse>> searchPlots(
            @RequestParam Long farmId,
            @RequestParam(required = false) Long productionAreaId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Long currentUserId = securityUtils.getRequiredCurrentUserId();
        List<PlotResponse> responses = plotService.searchPlots(currentUserId, farmId, productionAreaId, status, keyword);
        return ApiResponse.ok("Danh sách thửa đất", responses);
    }
}
