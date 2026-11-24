package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.cultivation.LotTraceabilityRespDTO;
import com.mycropdiary.api.service.cultivation.TraceabilityService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/harvest-lots")
@Tag(name = "10.2. Truy xuất nguồn gốc lô thu hoạch (Traceability)", description = "APIs truy xuất nguồn gốc nông sản theo lô thu hoạch")
@SecurityRequirement(name = "BearerAuthentication")
public class TraceabilityController {

    private final TraceabilityService traceabilityService;
    private final SecurityUtils securityUtils;

    public TraceabilityController(TraceabilityService traceabilityService, SecurityUtils securityUtils) {
        this.traceabilityService = traceabilityService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Xem thông tin truy xuất nguồn gốc lô thu hoạch", description = "Trả về chuỗi thông tin nguồn gốc của lô thu hoạch.")
    @GetMapping("/{lotId}/traceability")
    public ApiResp<LotTraceabilityRespDTO> getLotTraceability(@PathVariable Integer lotId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        return ApiResp.ok("Thông tin truy xuất nguồn gốc lô thu hoạch.", traceabilityService.getLotTraceability(currentUserId, lotId));
    }
}
