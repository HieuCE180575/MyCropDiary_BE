package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateMaterialUsageReq;
import com.mycropdiary.api.dto.cultivation.MaterialUsageResp;
import org.springframework.data.domain.Pageable;


/**
 * Interface service quản lý nghiệp vụ Material Usage (UC-25).
 */
public interface MaterialUsageService {

    /**
     * Tạo mới nhật ký sử dụng vật tư.
     */
    MaterialUsageResp createMaterialUsage(Integer userId, CreateMaterialUsageReq request);

    /**
     * Xem danh sách sử dụng vật tư theo hoạt động canh tác (có phân trang).
     */
    PageResp<MaterialUsageResp> getMaterialUsagesByActivity(Integer userId, Integer farmingActivityId, Pageable pageable);

    /**
     * Xem chi tiết một nhật ký sử dụng vật tư.
     */
    MaterialUsageResp getMaterialUsageById(Integer userId, Integer materialUsageId);
}
