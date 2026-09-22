package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/modules")
public class ModuleCatalogController {
    @GetMapping
    public ApiResponse<List<String>> listModules() {
        return ApiResponse.ok("MyCropDiary UC modules", List.of(
                "auth-profile", "farm-registration", "farm-members", "production-areas", "plots",
                "environmental-assessments", "crop-seasons", "tasks", "farming-activities",
                "materials-purchases-expenses", "harvest-traceability", "training",
                "vietgap-checklists", "internal-assessments", "reports", "ai-assistance", "admin"));
    }
}
