package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.block.BlockConRespDTO;
import com.mycropdiary.api.dto.block.BlockRespDTO;
import com.mycropdiary.api.dto.block.CreateBlockReqDTO;
import com.mycropdiary.api.dto.block.RecordBlockConReqDTO;
import com.mycropdiary.api.dto.block.UpdateBlockReqDTO;

import java.util.List;

/**
 * Service quản lý Lô / Thửa đất (Block) - UC-17: Manage Blocks.
 */
public interface BlockService {
    // UC-17: Tạo mới lô / thửa đất
    BlockRespDTO createBlock(Integer currentUserId, Integer zoneId, CreateBlockReqDTO request);

    // UC-17: Cập nhật lô / thửa đất (RU)
    BlockRespDTO updateBlock(Integer currentUserId, Integer blockId, UpdateBlockReqDTO request);

    // UC-17: Xem chi tiết lô / thửa đất
    BlockRespDTO getBlockById(Integer currentUserId, Integer blockId);

    // UC-17.1, 17.2, 17.3: Xem, Tìm kiếm và Lọc lô / thửa đất
    List<BlockRespDTO> searchBlocks(Integer currentUserId, Integer farmId, Integer zoneId, String status, String keyword);

    // UC-17: Xóa lô / thửa đất
    void deleteBlock(Integer currentUserId, Integer blockId);

    // UC-17.4: Ghi nhận tình trạng lô đất (Record Block Condition)
    BlockConRespDTO recordBlockCondition(Integer currentUserId, Integer blockId, RecordBlockConReqDTO request);

    // UC-17.4: Xem lịch sử tình trạng lô đất
    List<BlockConRespDTO> getBlockConditions(Integer currentUserId, Integer blockId);
}
