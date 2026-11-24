package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateCropSeasonReqDTO;
import com.mycropdiary.api.dto.cultivation.CropCategoryRespDTO;
import com.mycropdiary.api.dto.cultivation.CropSeasonRespDTO;
import com.mycropdiary.api.dto.cultivation.UpdateCropSeasonReqDTO;
import com.mycropdiary.api.dto.cultivation.UpdateCropSeasonStatusReqDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interface định nghĩa dịch vụ nghiệp vụ quản lý Mùa vụ canh tác (Crop Season).
 */
public interface CropSeasonService {

    /**
     * Tạo mùa vụ mới cho trang trại (chỉ Owner/Admin trang trại).
     */
    CropSeasonRespDTO createCropSeason(Integer userId, Integer farmId, CreateCropSeasonReqDTO request);

    /**
     * Cập nhật thông tin mùa vụ canh tác (chỉ Owner/Admin trang trại).
     */
    CropSeasonRespDTO updateCropSeason(Integer userId, Integer cropSeasonId, UpdateCropSeasonReqDTO request);

    /**
     * Cập nhật trạng thái mùa vụ (planned, active, completed, cancelled).
     */
    CropSeasonRespDTO updateCropSeasonStatus(Integer userId, Integer cropSeasonId, UpdateCropSeasonStatusReqDTO request);

    /**
     * Xem thông tin chi tiết mùa vụ canh tác.
     */
    CropSeasonRespDTO getCropSeasonById(Integer userId, Integer cropSeasonId);

    /**
     * Lấy danh sách mùa vụ của trang trại có phân trang, tìm kiếm và lọc.
     */
    PageResp<CropSeasonRespDTO> getCropSeasons(Integer userId, Integer farmId, String keyword, String status, Integer cropCategoryId, Pageable pageable);

    /**
     * Xóa mùa vụ canh tác (chỉ xóa được khi chưa có hoạt động hoặc thu hoạch liên kết).
     */
    void deleteCropSeason(Integer userId, Integer cropSeasonId);

    /**
     * Lấy danh mục các loại cây trồng khả dụng.
     */
    List<CropCategoryRespDTO> getCropCategories();
}

