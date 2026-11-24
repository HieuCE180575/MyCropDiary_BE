package com.mycropdiary.api.service.farmregistration;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.farm.AdminFarmRegRespDTO;
import com.mycropdiary.api.dto.farm.HandleFarmRegReqDTO;
import org.springframework.data.domain.Pageable;

public interface AdminFarmRegService {
    PageResp<AdminFarmRegRespDTO> getRegistrations(String status, Pageable pageable);
    AdminFarmRegRespDTO getRegistrationById(Integer registrationId);
    AdminFarmRegRespDTO handleRegistration(Integer adminUserId, Integer registrationId, HandleFarmRegReqDTO request);
}
