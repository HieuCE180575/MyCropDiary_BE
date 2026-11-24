package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.vietgap.ConductAssessmentReqDTO;
import com.mycropdiary.api.dto.vietgap.AssessmentRespDTO;

import java.util.List;

public interface InternalAssessmentService {

    /**
     * Conduct Internal VietGAP Assessment: Thực hiện đợt thẩm định / đánh giá nội bộ theo chuẩn VietGAP
     */
    AssessmentRespDTO conductInternalAssessment(Integer currentUserId, Integer farmId, ConductAssessmentReqDTO request);

    /**
     * View Internal Assessment History: Xem lịch sử các đợt đánh giá nội bộ của trang trại
     */
    List<AssessmentRespDTO> getInternalAssessmentHistory(Integer currentUserId, Integer farmId, Integer cropSeasonId, Integer zoneId);

    /**
     * View Internal Assessment Details: Xem chi tiết phiếu đánh giá nội bộ, điểm số các tiêu chí và bằng chứng thẩm định
     */
    AssessmentRespDTO getInternalAssessmentDetails(Integer currentUserId, Integer assessmentId);
}

