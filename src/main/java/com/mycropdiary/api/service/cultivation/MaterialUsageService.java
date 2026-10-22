package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateMaterialUsageReq;
import com.mycropdiary.api.dto.cultivation.MaterialUsageResp;
import com.mycropdiary.api.dto.cultivation.UpdateMaterialUsageReq;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Interface service quản lý nghiệp vụ Material Usage (UC-25).
 */
public interface MaterialUsageService {

    /**
     * Tạo mới nhật ký sử dụng vật tư.
     */
    MaterialUsageResp createMaterialUsage(Integer userId, CreateMaterialUsageReq request);

    /**
     * Cập nhật nhật ký sử dụng vật tư (UC-25.3).
     */
    MaterialUsageResp updateMaterialUsage(Integer userId, Integer materialUsageId, UpdateMaterialUsageReq request);

    /**
     * Xem danh sách sử dụng vật tư theo hoạt động canh tác (có phân trang).
     */
    PageResp<MaterialUsageResp> getMaterialUsagesByActivity(Integer userId, Integer farmingActivityId, Pageable pageable);

    /**
     * Tìm kiếm và lọc danh sách sử dụng vật tư (có phân trang).
     */
    PageResp<MaterialUsageResp> searchAndFilterMaterialUsages(
            Integer userId,
            Integer farmingActivityId,
            Integer cropSeasonId,
            String keyword,
            String materialType,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    );

    /**
     * Xem chi tiết một nhật ký sử dụng vật tư.
     */
    MaterialUsageResp getMaterialUsageById(Integer userId, Integer materialUsageId);
}
