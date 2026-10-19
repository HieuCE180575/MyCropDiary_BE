package com.mycropdiary.api.service.farmregistration;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.farm.HandleRegistrationRequest;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationCreateRequest;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationResponse;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationSummaryResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service định nghĩa các thao tác quản lý đơn đăng ký trang trại cho cả User scope (UC-09) và Admin scope (UC-39).
 */
public interface FarmRegistrationService {

    // ==================== User Scope (UC-09) ====================
    FarmRegistrationResponse createRegistration(UUID currentUserId, FarmRegistrationCreateRequest request);

    PageResponse<FarmRegistrationSummaryResponse> getMyRegistrations(UUID currentUserId, Pageable pageable);

    FarmRegistrationResponse getMyRegistrationDetail(UUID currentUserId, UUID registrationId);

    FarmRegistrationResponse cancelRegistration(UUID currentUserId, UUID registrationId);

    // ==================== Admin Scope (UC-39) ====================
    PageResponse<com.mycropdiary.api.dto.farm.FarmRegistrationResponse> getRegistrations(String status, Pageable pageable);

    com.mycropdiary.api.dto.farm.FarmRegistrationResponse getRegistrationById(UUID registrationId);

    com.mycropdiary.api.dto.farm.FarmRegistrationResponse handleRegistration(UUID adminUserId, UUID registrationId, HandleRegistrationRequest request);
}
