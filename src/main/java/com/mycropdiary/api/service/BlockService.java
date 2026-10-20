package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.block.BlockConditionResp;
import com.mycropdiary.api.dto.block.BlockResp;
import com.mycropdiary.api.dto.block.CreateBlockReq;
import com.mycropdiary.api.dto.block.RecordBlockConditionReq;
import com.mycropdiary.api.dto.block.UpdateBlockReq;

import java.util.List;

/**
 * Service quản lý Lô / Thửa đất (Block) - UC-17: Manage Blocks.
 */
public interface BlockService {
    // UC-17: Tạo mới lô / thửa đất
    BlockResp createBlock(Integer currentUserId, Integer zoneId, CreateBlockReq request);

    // UC-17: Cập nhật lô / thửa đất (RU)
    BlockResp updateBlock(Integer currentUserId, Integer blockId, UpdateBlockReq request);

    // UC-17: Xem chi tiết lô / thửa đất
    BlockResp getBlockById(Integer currentUserId, Integer blockId);

    // UC-17.1, 17.2, 17.3: Xem, Tìm kiếm và Lọc lô / thửa đất
    List<BlockResp> searchBlocks(Integer currentUserId, Integer farmId, Integer zoneId, String status, String keyword);

    // UC-17: Xóa lô / thửa đất
    void deleteBlock(Integer currentUserId, Integer blockId);

    // UC-17.4: Ghi nhận tình trạng lô đất (Record Block Condition)
    BlockConditionResp recordBlockCondition(Integer currentUserId, Integer blockId, RecordBlockConditionReq request);

    // UC-17.4: Xem lịch sử tình trạng lô đất
    List<BlockConditionResp> getBlockConditions(Integer currentUserId, Integer blockId);
}
