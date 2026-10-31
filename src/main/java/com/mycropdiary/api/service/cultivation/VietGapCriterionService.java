package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.vietgap.CreateVietGapCriterionReq;
import com.mycropdiary.api.dto.vietgap.UpdateVietGapCriterionReq;
import com.mycropdiary.api.dto.vietgap.VietGapCriterionResp;

import java.util.List;

public interface VietGapCriterionService {

    /**
     * Xem danh sách tiêu chuẩn đánh giá VietGAP của trang trại (View VietGAP Criterion List).
     */
    List<VietGapCriterionResp> getCriterionList(Integer currentUserId, Integer farmId, Boolean isActiveOnly, String keyword);

    /**
     * Xem chi tiết tiêu chuẩn và phương pháp đánh giá (View VietGAP Criterion Details).
     */
    VietGapCriterionResp getCriterionDetails(Integer currentUserId, Integer criterionId);

    /**
     * Tạo mới tiêu chuẩn đánh giá VietGAP theo TCVN 11892-1:2017 (Create VietGAP Criterion from TCVN 11892-1:2017).
     */
    VietGapCriterionResp createCriterion(Integer currentUserId, Integer farmId, CreateVietGapCriterionReq request);

    /**
     * Khởi tạo bộ tiêu chuẩn mặc định đầy đủ theo chuẩn quốc gia TCVN 11892-1:2017.
     */
    List<VietGapCriterionResp> importTcvnStandardCriteria(Integer currentUserId, Integer farmId);

    /**
     * Cập nhật tiêu chuẩn và phương pháp đánh giá (Update VietGAP Criterion and Evaluation Method).
     */
    VietGapCriterionResp updateCriterion(Integer currentUserId, Integer criterionId, UpdateVietGapCriterionReq request);

    /**
     * Xóa tiêu chuẩn đánh giá.
     */
    void deleteCriterion(Integer currentUserId, Integer criterionId);
}
