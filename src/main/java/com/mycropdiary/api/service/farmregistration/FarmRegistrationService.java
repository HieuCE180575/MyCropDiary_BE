package com.mycropdiary.api.service.farmregistration;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.farm.HandleRegistrationReq;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationCreateReq;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationResp;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationSummaryResp;
import org.springframework.data.domain.Pageable;


/**
 * Service định nghĩa các thao tác quản lý đơn đăng ký trang trại cho cả User scope (UC-09) và Admin scope (UC-39).
 */
public interface FarmRegistrationService {

    // ==================== User Scope (UC-09) ====================
    FarmRegistrationResp createRegistration(Integer currentUserId, FarmRegistrationCreateReq request);

    PageResp<FarmRegistrationSummaryResp> getMyRegistrations(Integer currentUserId, Pageable pageable);

    FarmRegistrationResp getMyRegistrationDetail(Integer currentUserId, Integer registrationId);

    FarmRegistrationResp cancelRegistration(Integer currentUserId, Integer registrationId);

    // ==================== Admin Scope (UC-39) ====================
    PageResp<com.mycropdiary.api.dto.farm.FarmRegistrationResp> getRegistrations(String status, Pageable pageable);

    com.mycropdiary.api.dto.farm.FarmRegistrationResp getRegistrationById(Integer registrationId);

    com.mycropdiary.api.dto.farm.FarmRegistrationResp handleRegistration(Integer adminUserId, Integer registrationId, HandleRegistrationReq request);
}
