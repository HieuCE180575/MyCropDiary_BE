package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.block.BlockResponse;
import com.mycropdiary.api.dto.block.CreateBlockRequest;
import com.mycropdiary.api.dto.block.UpdateBlockRequest;
import com.mycropdiary.api.dto.common.ApiResponse;
import com.mycropdiary.api.service.BlockService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Block Management", description = "Quản lý lô / thửa đất (Block)")
@RestController
@RequestMapping("/api/v1")
public class BlockController {
    private final BlockService blockService;
    private final SecurityUtils securityUtils;

    public BlockController(BlockService blockService, SecurityUtils securityUtils) {
        this.blockService = blockService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Tạo mới lô / thửa đất trong vùng (Block)")
    @PostMapping({"/zones/{zoneId}/blocks", "/production-areas/{zoneId}/plots"})
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BlockResponse> createBlock(
            @PathVariable UUID zoneId,
            @Valid @RequestBody CreateBlockRequest request) {
        UUID currentUserId = securityUtils.getRequiredCurrentUserId();
        BlockResponse response = blockService.createBlock(currentUserId, zoneId, request);
        return ApiResponse.ok("Tạo lô / thửa đất thành công", response);
    }

    @Operation(summary = "Lấy thông tin chi tiết lô / thửa đất (Block)")
    @GetMapping({"/blocks/{blockId}", "/plots/{blockId}"})
    public ApiResponse<BlockResponse> getBlockById(@PathVariable UUID blockId) {
        UUID currentUserId = securityUtils.getRequiredCurrentUserId();
        BlockResponse response = blockService.getBlockById(currentUserId, blockId);
        return ApiResponse.ok("Thông tin chi tiết lô / thửa đất", response);
    }

    @Operation(summary = "Cập nhật thông tin lô / thửa đất (Block)")
    @PutMapping({"/blocks/{blockId}", "/plots/{blockId}"})
    public ApiResponse<BlockResponse> updateBlock(
            @PathVariable UUID blockId,
            @Valid @RequestBody UpdateBlockRequest request) {
        UUID currentUserId = securityUtils.getRequiredCurrentUserId();
        BlockResponse response = blockService.updateBlock(currentUserId, blockId, request);
        return ApiResponse.ok("Cập nhật lô / thửa đất thành công", response);
    }

    @Operation(summary = "Xóa lô / thửa đất (Block)")
    @DeleteMapping({"/blocks/{blockId}", "/plots/{blockId}"})
    public ApiResponse<Void> deleteBlock(@PathVariable UUID blockId) {
        UUID currentUserId = securityUtils.getRequiredCurrentUserId();
        blockService.deleteBlock(currentUserId, blockId);
        return ApiResponse.ok("Đã xóa lô / thửa đất", null);
    }

    @Operation(summary = "Tìm kiếm danh sách lô / thửa đất (Block)")
    @GetMapping({"/blocks", "/plots"})
    public ApiResponse<List<BlockResponse>> searchBlocks(
            @RequestParam UUID farmId,
            @RequestParam(required = false) UUID zoneId,
            @RequestParam(required = false) UUID productionAreaId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        UUID currentUserId = securityUtils.getRequiredCurrentUserId();
        UUID targetZoneId = zoneId != null ? zoneId : productionAreaId;
        List<BlockResponse> responses = blockService.searchBlocks(currentUserId, farmId, targetZoneId, status, keyword);
        return ApiResponse.ok("Danh sách lô / thửa đất", responses);
    }
}
