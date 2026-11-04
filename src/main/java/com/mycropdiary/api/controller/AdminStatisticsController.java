package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.admin.AdminStatisticsResp;
import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.service.admin.AdminStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/statistics")
@Tag(name = "03. Quản trị - Thống kê hệ thống (Admin)", description = "APIs cung cấp số liệu tổng quan toàn hệ thống cho Quản trị viên")
@SecurityRequirement(name = "BearerAuthentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    public AdminStatisticsController(AdminStatisticsService adminStatisticsService) {
        this.adminStatisticsService = adminStatisticsService;
    }

    @Operation(summary = "Xem thống kê tổng quan toàn hệ thống",
            description = "Trả về số liệu thống kê chi tiết về tài khoản, hồ sơ đăng ký, trang trại, mùa vụ và phản hồi AI.")
    @GetMapping("/overview")
    public ApiResp<AdminStatisticsResp> getSystemStatistics() {
        AdminStatisticsResp statistics = adminStatisticsService.getSystemStatistics();
        return ApiResp.ok("Báo cáo thống kê toàn hệ thống", statistics);
    }
}

