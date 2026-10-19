package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.block.BlockResp;
import com.mycropdiary.api.dto.block.CreateBlockReq;
import com.mycropdiary.api.dto.block.UpdateBlockReq;
import com.mycropdiary.api.dto.common.ApiResp;
import com.mycropdiary.api.service.BlockService;
import com.mycropdiary.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ApiResp<BlockResp> createBlock(
            @PathVariable Integer zoneId,
            @Valid @RequestBody CreateBlockReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        BlockResp response = blockService.createBlock(currentUserId, zoneId, request);
        return ApiResp.ok("Tạo lô / thửa đất thành công", response);
    }

    @Operation(summary = "Lấy thông tin chi tiết lô / thửa đất (Block)")
    @GetMapping({"/blocks/{blockId}", "/plots/{blockId}"})
    public ApiResp<BlockResp> getBlockById(@PathVariable Integer blockId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        BlockResp response = blockService.getBlockById(currentUserId, blockId);
        return ApiResp.ok("Thông tin chi tiết lô / thửa đất", response);
    }

    @Operation(summary = "Cập nhật thông tin lô / thửa đất (Block)")
    @PutMapping({"/blocks/{blockId}", "/plots/{blockId}"})
    public ApiResp<BlockResp> updateBlock(
            @PathVariable Integer blockId,
            @Valid @RequestBody UpdateBlockReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        BlockResp response = blockService.updateBlock(currentUserId, blockId, request);
        return ApiResp.ok("Cập nhật lô / thửa đất thành công", response);
    }

    @Operation(summary = "Xóa lô / thửa đất (Block)")
    @DeleteMapping({"/blocks/{blockId}", "/plots/{blockId}"})
    public ApiResp<Void> deleteBlock(@PathVariable Integer blockId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        blockService.deleteBlock(currentUserId, blockId);
        return ApiResp.ok("Đã xóa lô / thửa đất", null);
    }

    @Operation(summary = "Tìm kiếm danh sách lô / thửa đất (Block)")
    @GetMapping({"/blocks", "/plots"})
    public ApiResp<List<BlockResp>> searchBlocks(
            @RequestParam Integer farmId,
            @RequestParam(required = false) Integer zoneId,
            @RequestParam(required = false) Integer productionAreaId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        Integer targetZoneId = zoneId != null ? zoneId : productionAreaId;
        List<BlockResp> responses = blockService.searchBlocks(currentUserId, farmId, targetZoneId, status, keyword);
        return ApiResp.ok("Danh sách lô / thửa đất", responses);
    }
}
