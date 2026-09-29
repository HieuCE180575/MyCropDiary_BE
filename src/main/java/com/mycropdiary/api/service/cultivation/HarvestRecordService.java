package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.cultivation.CreateHarvestRecordRequest;
import com.mycropdiary.api.dto.cultivation.HarvestRecordResponse;
import org.springframework.data.domain.Pageable;

/**
 * Interface service quản lý bản ghi thu hoạch (UC-26).
 */
public interface HarvestRecordService {

    /**
     * Tạo mới bản ghi thu hoạch sản phẩm.
     */
    HarvestRecordResponse createHarvestRecord(Long userId, CreateHarvestRecordRequest request);

    /**
     * Xem danh sách lịch sử thu hoạch theo Mùa vụ (có phân trang).
     */
    PageResponse<HarvestRecordResponse> getHarvestRecordsBySeason(Long userId, Long cropSeasonId, Pageable pageable);

    /**
     * Xem chi tiết một bản ghi thu hoạch theo ID.
     */
    HarvestRecordResponse getHarvestRecordById(Long userId, Long harvestRecordId);
}
