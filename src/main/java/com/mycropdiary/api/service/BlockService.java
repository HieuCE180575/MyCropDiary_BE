package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.block.CreateBlockReq;
import com.mycropdiary.api.dto.block.BlockResp;
import com.mycropdiary.api.dto.block.UpdateBlockReq;

import java.util.List;

/**
 * Service quản lý Lô / Thửa đất (Block).
 */
public interface BlockService {
    BlockResp createBlock(Integer currentUserId, Integer zoneId, CreateBlockReq request);
    BlockResp updateBlock(Integer currentUserId, Integer blockId, UpdateBlockReq request);
    BlockResp getBlockById(Integer currentUserId, Integer blockId);
    List<BlockResp> searchBlocks(Integer currentUserId, Integer farmId, Integer zoneId, String status, String keyword);
    void deleteBlock(Integer currentUserId, Integer blockId);
}
