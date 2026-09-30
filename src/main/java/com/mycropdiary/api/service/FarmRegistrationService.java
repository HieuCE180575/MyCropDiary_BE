package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.farm.FarmRegistrationResponse;
import com.mycropdiary.api.dto.farm.HandleRegistrationRequest;
import org.springframework.data.domain.Pageable;

// [AI_CHANGE] Root cause: UC-39 cần Service interface cho Admin duyệt/từ chối đơn đăng ký trang trại
// [AI_CHANGE] Mechanism: Định nghĩa 3 phương thức: danh sách phân trang, chi tiết, xử lý duyệt/từ chối
public interface FarmRegistrationService {

    /**
     * UC-39: Lấy danh sách đơn đăng ký trang trại với phân trang và lọc theo status.
     * Chỉ Admin mới có quyền gọi.
     *
     * @param status Lọc theo trạng thái (PENDING, APPROVED, REJECTED, CANCELLED) hoặc null để lấy tất cả
     * @param pageable Thông tin phân trang
     * @return Danh sách đơn đăng ký phân trang
     */
    PageResponse<FarmRegistrationResponse> getRegistrations(String status, Pageable pageable);

    /**
     * UC-39: Lấy chi tiết đơn đăng ký trang trại theo ID.
     *
     * @param registrationId ID đơn đăng ký
     * @return Thông tin chi tiết đơn đăng ký
     */
    FarmRegistrationResponse getRegistrationById(Long registrationId);

    /**
     * UC-39: Duyệt hoặc từ chối đơn đăng ký trang trại.
     * Khi duyệt (APPROVED): Tạo Farm mới + gán FarmMember OWNER cho người nộp đơn.
     * Khi từ chối (REJECTED): Cập nhật status và rejectionReason.
     * Chặn xử lý trùng: Chỉ xử lý đơn có status PENDING.
     *
     * @param adminUserId ID admin đang xử lý
     * @param registrationId ID đơn đăng ký
     * @param request Thông tin hành động (APPROVED/REJECTED + lý do từ chối)
     * @return Thông tin đơn đăng ký sau khi xử lý
     */
    FarmRegistrationResponse handleRegistration(Long adminUserId, Long registrationId, HandleRegistrationRequest request);
}
