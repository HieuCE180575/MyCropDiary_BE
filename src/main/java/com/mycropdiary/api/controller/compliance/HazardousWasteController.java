package com.mycropdiary.api.controller.compliance;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.compliance.HazardousWasteReqDTO;
import com.mycropdiary.api.dto.compliance.HazardousWasteRespDTO;
import com.mycropdiary.api.service.compliance.HazardousWasteService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "UC-47: Hazardous Waste Records (Mục 4.8 VietGAP)", description = "Quản lý rác thải nguy hại & vỏ bao bì thuốc BVTV (Dùng chung Staff + Owner)")
@SecurityRequirement(name = "BearerAuthentication")
@RestController
@RequestMapping("/api/v1")
public class HazardousWasteController {

    private final HazardousWasteService wasteService;
    private final SecurityUtils securityUtils;

    public HazardousWasteController(HazardousWasteService wasteService, SecurityUtils securityUtils) {
        this.wasteService = wasteService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "UC-47: Xem danh sách nhật ký rác thải nguy hại (Staff + Owner)")
    @GetMapping("/farms/{farmId}/waste-records")
    public ApiResp<List<HazardousWasteRespDTO>> getWasteLogs(@PathVariable Integer farmId) {
        Integer userId = securityUtils.getRequiredCurrentUserId();
        return ApiResp.ok("Danh sách nhật ký rác thải nguy hại", wasteService.getWasteLogs(userId, farmId));
    }

    @Operation(summary = "UC-47: Ghi nhận thu gom / bàn giao rác thải nguy hại (Staff + Owner)")
    @PostMapping("/farms/{farmId}/waste-records")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<HazardousWasteRespDTO> createWasteLog(
            @PathVariable Integer farmId,
            @Valid @RequestBody HazardousWasteReqDTO request) {
        Integer userId = securityUtils.getRequiredCurrentUserId();
        return ApiResp.ok("Ghi nhận rác thải nguy hại thành công", wasteService.createWasteLog(userId, farmId, request));
    }

    @Operation(summary = "UC-47: Cập nhật thông tin / chứng từ bàn giao rác thải (Staff + Owner)")
    @PutMapping("/waste-records/{wasteLogId}")
    public ApiResp<HazardousWasteRespDTO> updateWasteLog(
            @PathVariable Integer wasteLogId,
            @Valid @RequestBody HazardousWasteReqDTO request) {
        Integer userId = securityUtils.getRequiredCurrentUserId();
        return ApiResp.ok("Cập nhật nhật ký rác thải thành công", wasteService.updateWasteLog(userId, wasteLogId, request));
    }

    @Operation(summary = "UC-47: Xóa nhật ký rác thải nguy hại (Staff + Owner)")
    @DeleteMapping("/waste-records/{wasteLogId}")
    public ApiResp<Void> deleteWasteLog(@PathVariable Integer wasteLogId) {
        Integer userId = securityUtils.getRequiredCurrentUserId();
        wasteService.deleteWasteLog(userId, wasteLogId);
        return ApiResp.ok("Đã xóa nhật ký rác thải", null);
    }
}
