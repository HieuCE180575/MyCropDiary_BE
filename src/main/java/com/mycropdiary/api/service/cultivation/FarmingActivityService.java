package com.mycropdiary.api.service.cultivation;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.CreateFarmActivityReqDTO;
import com.mycropdiary.api.dto.cultivation.FarmingActivityRespDTO;
import com.mycropdiary.api.dto.cultivation.FarmingActivitySummaryRespDTO;
import com.mycropdiary.api.dto.cultivation.UpdateFarmingActivityReqDTO;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

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
    FarmingActivityRespDTO createActivity(Integer currentUserId, CreateFarmActivityReqDTO request);

    /**
     * Lấy danh sách lịch sử nhật ký canh tác theo Mùa vụ (Có phân trang).
     *
     * @param currentUserId ID người dùng đang đăng nhập
     * @param cropSeasonId ID mùa vụ canh tác
     * @param pageable Thông tin phân trang
     * @return Danh sách tóm tắt nhật ký canh tác dạng phân trang
     */
    PageResp<FarmingActivitySummaryRespDTO> getActivitiesByCropSeason(Integer currentUserId, Integer cropSeasonId, Pageable pageable);

    /**
     * Lấy danh sách nhật ký canh tác theo Mùa vụ kèm tìm kiếm và lọc nâng cao (UC-23.2 & UC-23.3).
     *
     * @param currentUserId ID người dùng đang đăng nhập
     * @param cropSeasonId ID mùa vụ canh tác
     * @param keyword Từ khóa tìm theo tên hoặc mô tả hoạt động
     * @param activityType Loại hoạt động canh tác
     * @param fromDate Thời gian bắt đầu
     * @param toDate Thời gian kết thúc
     * @param pageable Cấu hình phân trang
     * @return Danh sách tóm tắt nhật ký canh tác dạng phân trang
     */
    PageResp<FarmingActivitySummaryRespDTO> searchAndFilterActivities(
            Integer currentUserId,
            Integer cropSeasonId,
            String keyword,
            String activityType,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable
    );

    /**
     * Xem chi tiết một nhật ký canh tác theo ID.
     *
     * @param currentUserId ID người dùng đang đăng nhập
     * @param activityId ID nhật ký canh tác
     * @return Chi tiết nhật ký canh tác kèm danh sách công nhân thực hiện (nếu có)
     */
    FarmingActivityRespDTO getActivityDetail(Integer currentUserId, Integer activityId);

    /**
     * Cập nhật thông tin nhật ký canh tác (UC-24.3).
     *
     * @param currentUserId ID người dùng đang đăng nhập
     * @param activityId ID nhật ký canh tác
     * @param request Thông tin cập nhật nhật ký
     * @return Chi tiết nhật ký sau khi cập nhật
     */
    FarmingActivityRespDTO updateActivity(Integer currentUserId, Integer activityId, UpdateFarmingActivityReqDTO request);
}

