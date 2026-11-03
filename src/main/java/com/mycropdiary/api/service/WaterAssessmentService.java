package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.water.CreateWaterAssessmentReq;
import com.mycropdiary.api.dto.water.UpdateWaterAssessmentReq;
import com.mycropdiary.api.dto.water.WaterAssessmentResp;

import java.util.List;

public interface WaterAssessmentService {
    /**
     * Tạo mới đánh giá chất lượng nước theo Block
     */
    WaterAssessmentResp createWaterAssessment(Integer currentUserId, Integer blockId, CreateWaterAssessmentReq request);

    /**
     * Xem lịch sử đánh giá nước của Block (UC-20.1)
     */
    List<WaterAssessmentResp> getWaterAssessmentHistory(Integer currentUserId, Integer blockId);

    /**
     * Xem lịch sử đánh giá nguồn nước toàn trang trại (UC-20.1)
     */
    List<WaterAssessmentResp> getFarmWaterAssessmentHistory(Integer currentUserId, Integer farmId);

    /**
     * Xem chi tiết đánh giá nguồn nước
     */
    WaterAssessmentResp getWaterAssessmentDetails(Integer currentUserId, Integer assessmentId);

    /**
     * Cập nhật phiếu đánh giá nước
     */
    WaterAssessmentResp updateWaterAssessment(Integer currentUserId, Integer assessmentId, UpdateWaterAssessmentReq request);

    /**
     * Xóa phiếu đánh giá nguồn nước
     */
    void deleteWaterAssessment(Integer currentUserId, Integer assessmentId);
}

