package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateCropSeasonReq;
import com.mycropdiary.api.dto.cultivation.CropCategoryResp;
import com.mycropdiary.api.dto.cultivation.CropSeasonResp;
import com.mycropdiary.api.dto.cultivation.UpdateCropSeasonReq;
import com.mycropdiary.api.dto.cultivation.UpdateCropSeasonStatusReq;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interface định nghĩa dịch vụ nghiệp vụ quản lý Mùa vụ canh tác (Crop Season).
 */
public interface CropSeasonService {

    /**
     * Tạo mùa vụ mới cho trang trại (chỉ Owner/Admin trang trại).
     */
    CropSeasonResp createCropSeason(Integer userId, Integer farmId, CreateCropSeasonReq request);

    /**
     * Cập nhật thông tin mùa vụ canh tác (chỉ Owner/Admin trang trại).
     */
    CropSeasonResp updateCropSeason(Integer userId, Integer cropSeasonId, UpdateCropSeasonReq request);

    /**
     * Cập nhật trạng thái mùa vụ (planned, active, completed, cancelled).
     */
    CropSeasonResp updateCropSeasonStatus(Integer userId, Integer cropSeasonId, UpdateCropSeasonStatusReq request);

    /**
     * Xem thông tin chi tiết mùa vụ canh tác.
     */
    CropSeasonResp getCropSeasonById(Integer userId, Integer cropSeasonId);

    /**
     * Lấy danh sách mùa vụ của trang trại có phân trang, tìm kiếm và lọc.
     */
    PageResp<CropSeasonResp> getCropSeasons(Integer userId, Integer farmId, String keyword, String status, Integer cropCategoryId, Pageable pageable);

    /**
     * Xóa mùa vụ canh tác (chỉ xóa được khi chưa có hoạt động hoặc thu hoạch liên kết).
     */
    void deleteCropSeason(Integer userId, Integer cropSeasonId);

    /**
     * Lấy danh mục các loại cây trồng khả dụng.
     */
    List<CropCategoryResp> getCropCategories();
}

