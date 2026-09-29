package com.mycropdiary.api.service.farmregistration;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationCreateRequest;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationResponse;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationSummaryResponse;
import org.springframework.data.domain.Pageable;

/**
 * Service định nghĩa các thao tác quản lý đơn đăng ký trang trại từ phía người dùng (User scope).
 */
public interface FarmRegistrationService {

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
}
