package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.block.CreateBlockRequest;
import com.mycropdiary.api.dto.block.BlockResponse;
import com.mycropdiary.api.dto.block.UpdateBlockRequest;

import java.util.List;
import java.util.UUID;

/**
 * Service quản lý Lô / Thửa đất (Block).
 */
public interface BlockService {
    BlockResponse createBlock(UUID currentUserId, UUID zoneId, CreateBlockRequest request);
    BlockResponse updateBlock(UUID currentUserId, UUID blockId, UpdateBlockRequest request);
    BlockResponse getBlockById(UUID currentUserId, UUID blockId);
    List<BlockResponse> searchBlocks(UUID currentUserId, UUID farmId, UUID zoneId, String status, String keyword);
    void deleteBlock(UUID currentUserId, UUID blockId);
}
