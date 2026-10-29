package com.mycropdiary.api.controller.cultivation;

import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.dto.cultivation.AssignCropSeasonBlockReq;
import com.mycropdiary.api.dto.cultivation.CropSeasonBlockResp;
import com.mycropdiary.api.dto.cultivation.UpdateCropSeasonBlockReq;
import com.mycropdiary.api.service.cultivation.CropSeasonBlockService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/crop-seasons/{cropSeasonId}/blocks")
@Tag(name = "04.1. Phân bổ lô đất mùa vụ (Crop Season Blocks)", description = "APIs quản lý phân bổ và gán lô đất/luống canh tác vào mùa vụ")
@SecurityRequirement(name = "BearerAuthentication")
public class CropSeasonBlockController {

    private final CropSeasonBlockService cropSeasonBlockService;
    private final SecurityUtils securityUtils;

    public CropSeasonBlockController(CropSeasonBlockService cropSeasonBlockService, SecurityUtils securityUtils) {
        this.cropSeasonBlockService = cropSeasonBlockService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Gán lô đất vào mùa vụ", description = "Gán một lô đất vào mùa vụ canh tác kèm diện tích gieo trồng và ngày xuống giống/dự kiến thu hoạch.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<CropSeasonBlockResp> assignBlockToSeason(
            @PathVariable Integer cropSeasonId,
            @Valid @RequestBody AssignCropSeasonBlockReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        CropSeasonBlockResp response = cropSeasonBlockService.assignBlockToSeason(currentUserId, cropSeasonId, request);
        return ApiResp.ok("Gán lô đất vào mùa vụ thành công.", response);
    }

    @Operation(summary = "Danh sách lô đất trong mùa vụ", description = "Xem danh sách tất cả các lô đất đang được gieo trồng trong mùa vụ này.")
    @GetMapping
    public ApiResp<List<CropSeasonBlockResp>> getBlocksBySeason(@PathVariable Integer cropSeasonId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        List<CropSeasonBlockResp> response = cropSeasonBlockService.getBlocksBySeason(currentUserId, cropSeasonId);
        return ApiResp.ok("Danh sách lô đất trong mùa vụ.", response);
    }

    @Operation(summary = "Chi tiết lô đất trong mùa vụ", description = "Xem thông tin chi tiết của một lô đất cụ thể trong mùa vụ.")
    @GetMapping("/{blockId}")
    public ApiResp<CropSeasonBlockResp> getBlockInSeason(
            @PathVariable Integer cropSeasonId,
            @PathVariable Integer blockId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        CropSeasonBlockResp response = cropSeasonBlockService.getBlockInSeason(currentUserId, cropSeasonId, blockId);
        return ApiResp.ok("Chi tiết lô đất trong mùa vụ.", response);
    }

    @Operation(summary = "Cập nhật lô đất trong mùa vụ", description = "Chỉnh sửa diện tích gieo trồng, ngày xuống giống hoặc ngày dự kiến thu hoạch của lô đất trong mùa vụ.")
    @PutMapping("/{blockId}")
    public ApiResp<CropSeasonBlockResp> updateBlockInSeason(
            @PathVariable Integer cropSeasonId,
            @PathVariable Integer blockId,
            @Valid @RequestBody UpdateCropSeasonBlockReq request) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        CropSeasonBlockResp response = cropSeasonBlockService.updateBlockInSeason(currentUserId, cropSeasonId, blockId, request);
        return ApiResp.ok("Cập nhật thông tin lô đất trong mùa vụ thành công.", response);
    }

    @Operation(summary = "Gỡ lô đất khỏi mùa vụ", description = "Gỡ lô đất ra khỏi mùa vụ nếu chưa phát sinh nhật ký hoạt động canh tác hoặc thu hoạch tại lô này.")
    @DeleteMapping("/{blockId}")
    public ApiResp<Void> removeBlockFromSeason(
            @PathVariable Integer cropSeasonId,
            @PathVariable Integer blockId) {
        Integer currentUserId = securityUtils.getCurrentUserId();
        cropSeasonBlockService.removeBlockFromSeason(currentUserId, cropSeasonId, blockId);
        return ApiResp.ok("Gỡ lô đất khỏi mùa vụ thành công.", null);
    }
}
