package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.block.BlockConditionResp;
import com.mycropdiary.api.dto.block.BlockResp;
import com.mycropdiary.api.dto.block.CreateBlockReq;
import com.mycropdiary.api.dto.block.RecordBlockConditionReq;
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

@Tag(name = "UC-17: Block Management", description = "Quản lý Lô / Thửa đất sản xuất trong Zone (Block)")
@RestController
@RequestMapping("/api/v1")
public class BlockController {
    private final BlockService blockService;
    private final SecurityUtils securityUtils;

    public BlockController(BlockService blockService, SecurityUtils securityUtils) {
        this.blockService = blockService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "UC-17: Tạo mới lô / thửa đất trong Zone (Block)")
    @PostMapping("/zones/{zoneId}/blocks")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<BlockResp> createBlock(
            @PathVariable Integer zoneId,
            @Valid @RequestBody CreateBlockReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        BlockResp response = blockService.createBlock(currentUserId, zoneId, request);
        return ApiResp.ok("Tạo lô / thửa đất thành công", response);
    }

    @Operation(summary = "UC-17: Lấy thông tin chi tiết lô / thửa đất (Block)")
    @GetMapping("/blocks/{blockId}")
    public ApiResp<BlockResp> getBlockById(@PathVariable Integer blockId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        BlockResp response = blockService.getBlockById(currentUserId, blockId);
        return ApiResp.ok("Thông tin chi tiết lô / thửa đất", response);
    }

    @Operation(summary = "UC-17: Cập nhật thông tin lô / thửa đất (Block - RU)")
    @PutMapping("/blocks/{blockId}")
    public ApiResp<BlockResp> updateBlock(
            @PathVariable Integer blockId,
            @Valid @RequestBody UpdateBlockReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        BlockResp response = blockService.updateBlock(currentUserId, blockId, request);
        return ApiResp.ok("Cập nhật lô / thửa đất thành công", response);
    }

    @Operation(summary = "UC-17: Xóa lô / thửa đất (Block)")
    @DeleteMapping("/blocks/{blockId}")
    public ApiResp<Void> deleteBlock(@PathVariable Integer blockId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        blockService.deleteBlock(currentUserId, blockId);
        return ApiResp.ok("Đã xóa lô / thửa đất", null);
    }

    @Operation(summary = "UC-17.1, 17.2, 17.3: Xem, Tìm kiếm và Lọc danh sách lô / thửa đất (Block)")
    @GetMapping("/blocks")
    public ApiResp<List<BlockResp>> searchBlocks(
            @RequestParam Integer farmId,
            @RequestParam(required = false) Integer zoneId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<BlockResp> responses = blockService.searchBlocks(currentUserId, farmId, zoneId, status, keyword);
        return ApiResp.ok("Danh sách lô / thửa đất", responses);
    }

    @Operation(summary = "UC-17.4: Ghi nhận tình trạng lô đất (Record Block Condition)")
    @PostMapping("/blocks/{blockId}/conditions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResp<BlockConditionResp> recordBlockCondition(
            @PathVariable Integer blockId,
            @Valid @RequestBody RecordBlockConditionReq request) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        BlockConditionResp response = blockService.recordBlockCondition(currentUserId, blockId, request);
        return ApiResp.ok("Ghi nhận tình trạng lô đất thành công", response);
    }

    @Operation(summary = "UC-17.4: Xem lịch sử tình trạng lô đất (Block Condition History)")
    @GetMapping("/blocks/{blockId}/conditions")
    public ApiResp<List<BlockConditionResp>> getBlockConditions(@PathVariable Integer blockId) {
        Integer currentUserId = securityUtils.getRequiredCurrentUserId();
        List<BlockConditionResp> list = blockService.getBlockConditions(currentUserId, blockId);
        return ApiResp.ok("Lịch sử tình trạng lô đất", list);
    }
}
