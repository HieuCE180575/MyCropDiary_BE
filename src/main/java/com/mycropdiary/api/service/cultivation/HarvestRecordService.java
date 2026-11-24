package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateHarvestRecordReqDTO;
import com.mycropdiary.api.dto.cultivation.HarvestRecordRespDTO;
import com.mycropdiary.api.dto.cultivation.UpdateHarvestRecordReqDTO;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Interface service quản lý bản ghi thu hoạch (UC-26).
 */
public interface HarvestRecordService {

    /**
     * Tạo mới bản ghi thu hoạch sản phẩm.
     */
    HarvestRecordRespDTO createHarvestRecord(Integer userId, CreateHarvestRecordReqDTO request);

    /**
     * Cập nhật bản ghi thu hoạch sản phẩm (UC-26.3).
     */
    HarvestRecordRespDTO updateHarvestRecord(Integer userId, Integer harvestRecordId, UpdateHarvestRecordReqDTO request);

    /**
     * Xem danh sách lịch sử thu hoạch theo Mùa vụ (có phân trang).
     */
    PageResp<HarvestRecordRespDTO> getHarvestRecordsBySeason(Integer userId, Integer cropSeasonId, Pageable pageable);

    /**
     * Tìm kiếm và lọc danh sách bản ghi thu hoạch theo mùa vụ (có phân trang, từ khóa, chất lượng, khoảng ngày).
     */
    PageResp<HarvestRecordRespDTO> searchAndFilterHarvestRecords(
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
    PageResp<HarvestRecordRespDTO> searchFarmHarvestRecords(
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
    HarvestRecordRespDTO getHarvestRecordById(Integer userId, Integer harvestRecordId);
}
