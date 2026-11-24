package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.water.CreateWaterReqDTO;
import com.mycropdiary.api.dto.water.UpdateWaterReqDTO;
import com.mycropdiary.api.dto.water.WaterAssessmentRespDTO;

import java.util.List;

public interface WaterAssessmentService {
    /**
     * Tạo mới đánh giá chất lượng nước theo Block
     */
    WaterAssessmentRespDTO createWaterAssessment(Integer currentUserId, Integer blockId, CreateWaterReqDTO request);

    /**
     * Xem lịch sử đánh giá nước của Block (UC-20.1)
     */
    List<WaterAssessmentRespDTO> getWaterAssessmentHistory(Integer currentUserId, Integer blockId);

    /**
     * Xem lịch sử đánh giá nguồn nước toàn trang trại (UC-20.1)
     */
    List<WaterAssessmentRespDTO> getFarmWaterAssessmentHistory(Integer currentUserId, Integer farmId);

    /**
     * Xem chi tiết đánh giá nguồn nước
     */
    WaterAssessmentRespDTO getWaterAssessmentDetails(Integer currentUserId, Integer assessmentId);

    /**
     * Cập nhật phiếu đánh giá nước
     */
    WaterAssessmentRespDTO updateWaterAssessment(Integer currentUserId, Integer assessmentId, UpdateWaterReqDTO request);

    /**
     * Xóa phiếu đánh giá nguồn nước
     */
    void deleteWaterAssessment(Integer currentUserId, Integer assessmentId);
}

