package com.mycropdiary.api.service.farmregistration;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.farm.HandleRegistrationRequest;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationCreateRequest;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationResponse;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationSummaryResponse;
import org.springframework.data.domain.Pageable;

/**
 * Service định nghĩa các thao tác quản lý đơn đăng ký trang trại cho cả User scope (UC-09) và Admin scope (UC-39).
 */
public interface FarmRegistrationService {

    // ==================== User Scope (UC-09) ====================

    /**
     * Gửi yêu cầu đăng ký trang trại mới.
     *
     * @param currentUserId ID người dùng đăng nhập
     * @param request Thông tin đăng ký
     * @return Thông tin đơn đăng ký vừa tạo (Status: PENDING)
     */
    FarmRegistrationResponse createRegistration(Long currentUserId, FarmRegistrationCreateRequest request);

    /**
     * Lấy danh sách đơn đăng ký trang trại của người dùng hiện tại (Có phân trang).
     *
     * @param currentUserId ID người dùng đăng nhập
     * @param pageable Thông tin phân trang
     * @return Danh sách đơn đăng ký dạng phân trang
     */
    PageResponse<FarmRegistrationSummaryResponse> getMyRegistrations(Long currentUserId, Pageable pageable);

    /**
     * Xem chi tiết đơn đăng ký trang trại thuộc sở hữu của người dùng hiện tại.
     *
     * @param currentUserId ID người dùng đăng nhập
     * @param registrationId ID đơn đăng ký
     * @return Chi tiết đơn đăng ký
     */
    FarmRegistrationResponse getMyRegistrationDetail(Long currentUserId, Long registrationId);

    /**
     * Hủy đơn đăng ký trang trại của người dùng hiện tại khi đang ở trạng thái PENDING.
     *
     * @param currentUserId ID người dùng đăng nhập
     * @param registrationId ID đơn đăng ký
     * @return Chi tiết đơn đăng ký sau khi đã hủy (Status: CANCELLED)
     */
    FarmRegistrationResponse cancelRegistration(Long currentUserId, Long registrationId);

    // ==================== Admin Scope (UC-39) ====================

    /**
     * Admin lấy danh sách đơn đăng ký trang trại với phân trang và lọc theo status.
     *
     * @param status Lọc theo trạng thái (PENDING, APPROVED, REJECTED, CANCELLED) hoặc null để lấy tất cả
     * @param pageable Thông tin phân trang
     * @return Danh sách đơn đăng ký phân trang
     */
    PageResponse<com.mycropdiary.api.dto.farm.FarmRegistrationResponse> getRegistrations(String status, Pageable pageable);

    /**
     * Admin lấy chi tiết đơn đăng ký trang trại theo ID.
     *
     * @param registrationId ID đơn đăng ký
     * @return Thông tin chi tiết đơn đăng ký cho admin
     */
    com.mycropdiary.api.dto.farm.FarmRegistrationResponse getRegistrationById(Long registrationId);

    /**
     * Admin duyệt hoặc từ chối đơn đăng ký trang trại.
     * Khi duyệt (APPROVED): Tạo Farm mới + gán FarmMember OWNER cho người nộp đơn.
     * Khi từ chối (REJECTED): Cập nhật status và rejectionReason.
     *
     * @param adminUserId ID admin đang xử lý
     * @param registrationId ID đơn đăng ký
     * @param request Thông tin hành động (APPROVED/REJECTED + lý do từ chối)
     * @return Thông tin đơn đăng ký sau khi xử lý
     */
    com.mycropdiary.api.dto.farm.FarmRegistrationResponse handleRegistration(Long adminUserId, Long registrationId, HandleRegistrationRequest request);
}
