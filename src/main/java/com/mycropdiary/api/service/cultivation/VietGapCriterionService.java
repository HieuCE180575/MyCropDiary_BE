package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.vietgap.CreateVietGapCriterionReqDTO;
import com.mycropdiary.api.dto.vietgap.UpdateVietGapCriterionReqDTO;
import com.mycropdiary.api.dto.vietgap.VietGapCriterionRespDTO;

import java.util.List;

public interface VietGapCriterionService {

    /**
     * Xem danh sách tiêu chuẩn đánh giá VietGAP của trang trại (View VietGAP Criterion List).
     */
    List<VietGapCriterionRespDTO> getCriterionList(Integer currentUserId, Integer farmId, Boolean isActiveOnly, String keyword);

    /**
     * Xem chi tiết tiêu chuẩn và phương pháp đánh giá (View VietGAP Criterion Details).
     */
    VietGapCriterionRespDTO getCriterionDetails(Integer currentUserId, Integer criterionId);

    /**
     * Tạo mới tiêu chuẩn đánh giá VietGAP theo TCVN 11892-1:2017 (Create VietGAP Criterion from TCVN 11892-1:2017).
     */
    VietGapCriterionRespDTO createCriterion(Integer currentUserId, Integer farmId, CreateVietGapCriterionReqDTO request);

    /**
     * Khởi tạo bộ tiêu chuẩn mặc định đầy đủ theo chuẩn quốc gia TCVN 11892-1:2017.
     */
    List<VietGapCriterionRespDTO> importTcvnStandardCriteria(Integer currentUserId, Integer farmId);

    /**
     * Cập nhật tiêu chuẩn và phương pháp đánh giá (Update VietGAP Criterion and Evaluation Method).
     */
    VietGapCriterionRespDTO updateCriterion(Integer currentUserId, Integer criterionId, UpdateVietGapCriterionReqDTO request);

    /**
     * Xóa tiêu chuẩn đánh giá.
     */
    void deleteCriterion(Integer currentUserId, Integer criterionId);
}

