package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.dto.farm.FarmSummaryResponse;
import com.mycropdiary.api.service.FarmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// [AI_CHANGE] OpenAPI documentation annotations for FarmController
@RestController
@RequestMapping("/api/v1/farms")
@Tag(name = "02. Trang trại (Farms)", description = "APIs quản lý trang trại và danh sách trang trại thành viên có quyền truy cập")
public class FarmController {
    private final FarmService farmService;

    public FarmController(FarmService farmService) {
        this.farmService = farmService;
    }

    @Operation(
            summary = "Lấy danh sách trang trại có quyền truy cập",
            description = "Trả về danh sách tất cả trang trại mà tài khoản hiện tại là thành viên (Yêu cầu JWT Bearer Token)",
            security = @SecurityRequirement(name = "BearerAuthentication")
    )
    @GetMapping
    public ApiResponse<List<FarmSummaryResponse>> findAll() {
        return ApiResponse.ok("Accessible farms", farmService.findAllAccessibleFarms());
    }
}
