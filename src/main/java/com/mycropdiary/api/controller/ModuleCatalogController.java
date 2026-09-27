package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// [AI_CHANGE] OpenAPI documentation annotations for ModuleCatalogController
@RestController
@RequestMapping("/api/v1/modules")
@Tag(name = "00. Hệ thống (System)", description = "APIs danh mục và thông tin modules của MyCropDiary")
public class ModuleCatalogController {
    @Operation(summary = "Danh sách modules UC hệ thống", description = "Trả về danh sách 17 modules Use Case theo thiết kế kiến trúc MyCropDiary")
    @GetMapping
    public ApiResponse<List<String>> listModules() {
        return ApiResponse.ok("MyCropDiary UC modules", List.of(
                "auth-profile", "farm-registration", "farm-members", "production-areas", "plots",
                "environmental-assessments", "crop-seasons", "tasks", "farming-activities",
                "materials-purchases-expenses", "harvest-traceability", "training",
                "vietgap-checklists", "internal-assessments", "reports", "ai-assistance", "admin"));
    }
}
