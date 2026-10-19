package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateFarmingActivityReq;
import com.mycropdiary.api.dto.cultivation.FarmingActivityResp;
import com.mycropdiary.api.dto.cultivation.FarmingActivitySummaryResp;
import org.springframework.data.domain.Pageable;


/**
 * Service định nghĩa các thao tác quản lý Nhật ký canh tác (UC-24).
 */
public interface FarmingActivityService {

    /**
     * Tạo mới nhật ký canh tác.
     *
     * @param currentUserId ID người dùng đang đăng nhập
     * @param request Thông tin nhật ký mới
     * @return Chi tiết nhật ký vừa tạo
     */
    FarmingActivityResp createActivity(Integer currentUserId, CreateFarmingActivityReq request);

    /**
     * Lấy danh sách lịch sử nhật ký canh tác theo Mùa vụ (Có phân trang).
     *
     * @param currentUserId ID người dùng đang đăng nhập
     * @param cropSeasonId ID mùa vụ canh tác
     * @param pageable Thông tin phân trang
     * @return Danh sách tóm tắt nhật ký canh tác dạng phân trang
     */
    PageResp<FarmingActivitySummaryResp> getActivitiesByCropSeason(Integer currentUserId, Integer cropSeasonId, Pageable pageable);

    /**
     * Xem chi tiết một nhật ký canh tác theo ID.
     *
     * @param currentUserId ID người dùng đang đăng nhập
     * @param activityId ID nhật ký canh tác
     * @return Chi tiết nhật ký canh tác kèm danh sách công nhân thực hiện (nếu có)
     */
    FarmingActivityResp getActivityDetail(Integer currentUserId, Integer activityId);
}
