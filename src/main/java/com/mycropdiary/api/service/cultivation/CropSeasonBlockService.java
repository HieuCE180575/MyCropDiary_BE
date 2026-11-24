package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.cultivation.AssignCropSeasonBlockReqDTO;
import com.mycropdiary.api.dto.cultivation.CropSeasonBlockRespDTO;
import com.mycropdiary.api.dto.cultivation.UpdateCropSeasonBlockReqDTO;

import java.util.List;

public interface CropSeasonBlockService {

    /**
     * Gán lô đất vào mùa vụ canh tác.
     */
    CropSeasonBlockRespDTO assignBlockToSeason(Integer userId, Integer cropSeasonId, AssignCropSeasonBlockReqDTO request);

    /**
     * Danh sách tất cả các lô đất được gán trong mùa vụ.
     */
    List<CropSeasonBlockRespDTO> getBlocksBySeason(Integer userId, Integer cropSeasonId);

    /**
     * Chi tiết gán lô đất trong mùa vụ.
     */
    CropSeasonBlockRespDTO getBlockInSeason(Integer userId, Integer cropSeasonId, Integer blockId);

    /**
     * Cập nhật diện tích, ngày gieo trồng hoặc ngày dự kiến thu hoạch của lô đất trong mùa vụ.
     */
    CropSeasonBlockRespDTO updateBlockInSeason(Integer userId, Integer cropSeasonId, Integer blockId, UpdateCropSeasonBlockReqDTO request);

    /**
     * Gỡ lô đất khỏi mùa vụ canh tác (khi chưa phát sinh nhật ký hoạt động / thu hoạch tại lô này).
     */
    void removeBlockFromSeason(Integer userId, Integer cropSeasonId, Integer blockId);
}
