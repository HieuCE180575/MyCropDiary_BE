package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.soil.CreateSoilReqDTO;
import com.mycropdiary.api.dto.soil.SoilAssessmentRespDTO;
import com.mycropdiary.api.dto.soil.UpdateSoilReqDTO;

import java.util.List;

public interface SoilAssessmentService {
    /**
     * Create Soil Assessment: Tạo mới đánh giá thổ nhưỡng lô đất
     */
    SoilAssessmentRespDTO createSoilAssessment(Integer currentUserId, Integer blockId, CreateSoilReqDTO request);

    /**
     * View Soil Assessment History: Xem lịch sử đánh giá thổ nhưỡng của lô đất
     */
    List<SoilAssessmentRespDTO> getSoilAssessmentHistory(Integer currentUserId, Integer blockId);

    /**
     * Xem lịch sử đánh giá thổ nhưỡng toàn trang trại
     */
    List<SoilAssessmentRespDTO> getFarmSoilAssessmentHistory(Integer currentUserId, Integer farmId);

    /**
     * View Soil Assessment Details: Xem chi tiết phiếu đánh giá thổ nhưỡng
     */
    SoilAssessmentRespDTO getSoilAssessmentDetails(Integer currentUserId, Integer assessmentId);

    /**
     * Update Soil Assessment: Cập nhật phiếu đánh giá thổ nhưỡng lô đất
     */
    SoilAssessmentRespDTO updateSoilAssessment(Integer currentUserId, Integer assessmentId, UpdateSoilReqDTO request);

    /**
     * Delete Eligible Soil Assessment: Xóa phiếu đánh giá thổ nhưỡng (yêu cầu quyền Farm Owner hoặc System Admin)
     */
    void deleteSoilAssessment(Integer currentUserId, Integer assessmentId);
}

