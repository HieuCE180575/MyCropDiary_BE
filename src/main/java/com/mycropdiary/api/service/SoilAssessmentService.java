package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.soil.CreateSoilAssessmentReq;
import com.mycropdiary.api.dto.soil.SoilAssessmentResp;
import com.mycropdiary.api.dto.soil.UpdateSoilAssessmentReq;

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
     * View Soil Assessment Details: Xem chi tiết phiếu đánh giá thổ nhưỡng
     */
    SoilAssessmentResp getSoilAssessmentDetails(Integer currentUserId, Integer assessmentId);

    /**
     * Update Soil Assessment: Cập nhật phiếu đánh giá thổ nhưỡng lô đất
     */
    SoilAssessmentResp updateSoilAssessment(Integer currentUserId, Integer assessmentId, UpdateSoilAssessmentReq request);

    /**
     * Delete Eligible Soil Assessment: Xóa phiếu đánh giá thổ nhưỡng (yêu cầu quyền Farm Owner hoặc System Admin)
     */
    void deleteSoilAssessment(Integer currentUserId, Integer assessmentId);
}

