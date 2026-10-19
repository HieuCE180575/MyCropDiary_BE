package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateHarvestRecordReq;
import com.mycropdiary.api.dto.cultivation.HarvestRecordResp;
import org.springframework.data.domain.Pageable;


/**
 * Interface service quản lý bản ghi thu hoạch (UC-26).
 */
public interface HarvestRecordService {

    /**
     * Tạo mới bản ghi thu hoạch sản phẩm.
     */
    HarvestRecordResp createHarvestRecord(Integer userId, CreateHarvestRecordReq request);

    /**
     * Xem danh sách lịch sử thu hoạch theo Mùa vụ (có phân trang).
     */
    PageResp<HarvestRecordResp> getHarvestRecordsBySeason(Integer userId, Integer cropSeasonId, Pageable pageable);

    /**
     * Xem chi tiết một bản ghi thu hoạch theo ID.
     */
    HarvestRecordResp getHarvestRecordById(Integer userId, Integer harvestRecordId);
}
