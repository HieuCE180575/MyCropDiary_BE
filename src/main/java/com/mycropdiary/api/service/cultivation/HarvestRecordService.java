package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateHarvestRecordReq;
import com.mycropdiary.api.dto.cultivation.HarvestRecordResp;
import com.mycropdiary.api.dto.cultivation.UpdateHarvestRecordReq;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Interface service quản lý bản ghi thu hoạch (UC-26).
 */
public interface HarvestRecordService {

    /**
     * Tạo mới bản ghi thu hoạch sản phẩm.
     */
    HarvestRecordResp createHarvestRecord(Integer userId, CreateHarvestRecordReq request);

    /**
     * Cập nhật bản ghi thu hoạch sản phẩm (UC-26.3).
     */
    HarvestRecordResp updateHarvestRecord(Integer userId, Integer harvestRecordId, UpdateHarvestRecordReq request);

    /**
     * Xem danh sách lịch sử thu hoạch theo Mùa vụ (có phân trang).
     */
    PageResp<HarvestRecordResp> getHarvestRecordsBySeason(Integer userId, Integer cropSeasonId, Pageable pageable);

    /**
     * Tìm kiếm và lọc danh sách bản ghi thu hoạch theo mùa vụ (có phân trang, từ khóa, chất lượng, khoảng ngày).
     */
    PageResp<HarvestRecordResp> searchAndFilterHarvestRecords(
            Integer userId,
            Integer cropSeasonId,
            String keyword,
            String qualityGrade,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable);

    /**
     * Tìm kiếm và lọc danh sách bản ghi thu hoạch trên toàn trang trại (hỗ trợ lọc theo mùa vụ, lô đất, chất lượng, từ khóa, khoảng ngày).
     */
    PageResp<HarvestRecordResp> searchFarmHarvestRecords(
            Integer userId,
            Integer farmId,
            Integer cropSeasonId,
            Integer blockId,
            String keyword,
            String qualityGrade,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable);

    /**
     * Xem chi tiết một bản ghi thu hoạch theo ID.
     */
    HarvestRecordResp getHarvestRecordById(Integer userId, Integer harvestRecordId);
}
