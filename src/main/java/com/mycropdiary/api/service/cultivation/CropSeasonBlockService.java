package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.cultivation.AssignCropSeasonBlockReq;
import com.mycropdiary.api.dto.cultivation.CropSeasonBlockResp;
import com.mycropdiary.api.dto.cultivation.UpdateCropSeasonBlockReq;

import java.util.List;

public interface CropSeasonBlockService {

    /**
     * Gán lô đất vào mùa vụ canh tác.
     */
    CropSeasonBlockResp assignBlockToSeason(Integer userId, Integer cropSeasonId, AssignCropSeasonBlockReq request);

    /**
     * Danh sách tất cả các lô đất được gán trong mùa vụ.
     */
    List<CropSeasonBlockResp> getBlocksBySeason(Integer userId, Integer cropSeasonId);

    /**
     * Chi tiết gán lô đất trong mùa vụ.
     */
    CropSeasonBlockResp getBlockInSeason(Integer userId, Integer cropSeasonId, Integer blockId);

    /**
     * Cập nhật diện tích, ngày gieo trồng hoặc ngày dự kiến thu hoạch của lô đất trong mùa vụ.
     */
    CropSeasonBlockResp updateBlockInSeason(Integer userId, Integer cropSeasonId, Integer blockId, UpdateCropSeasonBlockReq request);

    /**
     * Gỡ lô đất khỏi mùa vụ canh tác (khi chưa phát sinh nhật ký hoạt động / thu hoạch tại lô này).
     */
    void removeBlockFromSeason(Integer userId, Integer cropSeasonId, Integer blockId);
}
