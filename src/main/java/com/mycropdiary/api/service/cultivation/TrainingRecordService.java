package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.training.CreateTrainingReqDTO;
import com.mycropdiary.api.dto.training.TrainingRecordRespDTO;
import com.mycropdiary.api.dto.training.UpdateTrainingReqDTO;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface TrainingRecordService {

    /**
     * Tạo mới hồ sơ đào tạo / tập huấn VietGAP (Create Training Record).
     */
    TrainingRecordRespDTO createTrainingRecord(Integer currentUserId, Integer farmId, CreateTrainingReqDTO request);

    /**
     * Xem lịch sử đào tạo toàn trang trại (View Training History - hỗ trợ tìm kiếm từ khóa, khoảng ngày, phân trang).
     */
    PageResp<TrainingRecordRespDTO> getTrainingHistory(Integer currentUserId, Integer farmId, String keyword, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    /**
     * Xem chi tiết hồ sơ đào tạo kèm danh sách công nhân tham gia (View Training Record Details).
     */
    TrainingRecordRespDTO getTrainingRecordDetails(Integer currentUserId, Integer trainingRecordId);

    /**
     * Cập nhật hồ sơ đào tạo và danh sách điểm danh (Update Training Record).
     */
    TrainingRecordRespDTO updateTrainingRecord(Integer currentUserId, Integer trainingRecordId, UpdateTrainingReqDTO request);

    /**
     * Xóa hồ sơ đào tạo.
     */
    void deleteTrainingRecord(Integer currentUserId, Integer trainingRecordId);
}

