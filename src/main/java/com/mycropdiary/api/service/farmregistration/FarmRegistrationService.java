package com.mycropdiary.api.service.farmregistration;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationReqDTO;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationRespDTO;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationSummaryRespDTO;
import org.springframework.data.domain.Pageable;

/**
 * Service định nghĩa các thao tác quản lý đơn đăng ký trang trại cho User scope (UC-09).
 */
public interface FarmRegistrationService {

    FarmRegistrationRespDTO createRegistration(Integer currentUserId, FarmRegistrationReqDTO request);

    PageResp<FarmRegistrationSummaryRespDTO> getMyRegistrations(Integer currentUserId, Pageable pageable);

    FarmRegistrationRespDTO getMyRegistrationDetail(Integer currentUserId, Integer registrationId);

    FarmRegistrationRespDTO updateRegistration(Integer currentUserId, Integer registrationId, FarmRegistrationReqDTO request);

    FarmRegistrationRespDTO cancelRegistration(Integer currentUserId, Integer registrationId);
}
