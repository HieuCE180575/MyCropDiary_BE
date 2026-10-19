package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.cultivation.CreateMaterialUsageRequest;
import com.mycropdiary.api.dto.cultivation.MaterialUsageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Interface service quản lý nghiệp vụ Material Usage (UC-25).
 */
public interface MaterialUsageService {

    /**
     * Tạo mới nhật ký sử dụng vật tư.
     */
    MaterialUsageResponse createMaterialUsage(UUID userId, CreateMaterialUsageRequest request);

    /**
     * Xem danh sách sử dụng vật tư theo hoạt động canh tác (có phân trang).
     */
    PageResponse<MaterialUsageResponse> getMaterialUsagesByActivity(UUID userId, UUID farmingActivityId, Pageable pageable);

    /**
     * Xem chi tiết một nhật ký sử dụng vật tư.
     */
    MaterialUsageResponse getMaterialUsageById(UUID userId, UUID materialUsageId);
}
