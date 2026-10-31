package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.soil.CreateSoilAssessmentReq;
import com.mycropdiary.api.dto.soil.SoilAssessmentResp;

import java.util.List;

public interface SoilAssessmentService {
    /**
     * Create Soil Assessment: Tạo mới đánh giá thổ nhưỡng lô đất
     */
    SoilAssessmentResp createSoilAssessment(Integer currentUserId, Integer blockId, CreateSoilAssessmentReq request);

    /**
     * View Soil Assessment History: Xem lịch sử đánh giá thổ nhưỡng của lô đất
     */
    List<SoilAssessmentResp> getSoilAssessmentHistory(Integer currentUserId, Integer blockId);

    /**
     * Xem lịch sử đánh giá thổ nhưỡng toàn trang trại
     */
    List<SoilAssessmentResp> getFarmSoilAssessmentHistory(Integer currentUserId, Integer farmId);

    /**
     * Xem chi tiết phiếu đánh giá thổ nhưỡng
     */
    SoilAssessmentResp getSoilAssessmentDetails(Integer currentUserId, Integer assessmentId);

    /**
     * Xóa phiếu đánh giá thổ nhưỡng (yêu cầu quyền Farm Owner hoặc System Admin)
     */
    void deleteSoilAssessment(Integer currentUserId, Integer assessmentId);
}

