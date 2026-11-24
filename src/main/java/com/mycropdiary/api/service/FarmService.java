package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.farm.*;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface định nghĩa các dịch vụ nghiệp vụ cốt lõi cho trang trại (Farm Core).
 */
public interface FarmService {
    FarmRespDTO createFarm(Integer currentUserId, CreateFarmReqDTO request);

    FarmRespDTO getFarmById(Integer currentUserId, Integer farmId);

    PageResp<FarmSummaryRespDTO> searchFarms(Integer currentUserId, String keyword, String status, String province, Pageable pageable);

    PageResp<FarmSummaryRespDTO> searchFarms(
            Integer currentUserId,
            String keyword,
            String province,
            String status,
            BigDecimal minArea,
            BigDecimal maxArea,
            Pageable pageable
    );

    List<FarmSummaryRespDTO> findAllAccessibleFarms(Integer currentUserId);

    FarmRespDTO updateFarm(Integer currentUserId, Integer farmId, UpdateFarmReqDTO request);

    void deleteFarm(Integer currentUserId, Integer farmId);

    FarmMemberRespDTO addMember(Integer currentUserId, Integer farmId, AddFarmMemberReqDTO request);

    List<FarmMemberRespDTO> getFarmMembers(Integer currentUserId, Integer farmId);

    void removeMember(Integer currentUserId, Integer farmId, Integer memberId);

    StaffZoneAssignmentRespDTO assignStaffToZone(Integer currentUserId, Integer farmId, Integer memberId, AssignStaffZoneReqDTO request);

    ZoneRespDTO createZone(Integer currentUserId, Integer farmId, CreateZoneReqDTO request);

    List<ZoneRespDTO> getZones(Integer currentUserId, Integer farmId);
}
