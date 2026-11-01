package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.vietgap.ConductInternalAssessmentReq;
import com.mycropdiary.api.dto.vietgap.InternalAssessmentResp;

import java.util.List;

public interface InternalAssessmentService {

    /**
     * Conduct Internal VietGAP Assessment: Thực hiện đợt thẩm định / đánh giá nội bộ theo chuẩn VietGAP
     */
    InternalAssessmentResp conductInternalAssessment(Integer currentUserId, Integer farmId, ConductInternalAssessmentReq request);

    /**
     * View Internal Assessment History: Xem lịch sử các đợt đánh giá nội bộ của trang trại
     */
    List<InternalAssessmentResp> getInternalAssessmentHistory(Integer currentUserId, Integer farmId, Integer cropSeasonId, Integer zoneId);

    /**
     * View Internal Assessment Details: Xem chi tiết phiếu đánh giá nội bộ, điểm số các tiêu chí và bằng chứng thẩm định
     */
    InternalAssessmentResp getInternalAssessmentDetails(Integer currentUserId, Integer assessmentId);
}

