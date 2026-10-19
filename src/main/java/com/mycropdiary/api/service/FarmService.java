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
    FarmResp createFarm(Integer currentUserId, CreateFarmReq request);

    FarmResp getFarmById(Integer currentUserId, Integer farmId);

    PageResp<FarmSummaryResp> searchFarms(Integer currentUserId, String keyword, String status, String province, Pageable pageable);

    PageResp<FarmSummaryResp> searchFarms(
            Integer currentUserId,
            String keyword,
            String province,
            String status,
            BigDecimal minArea,
            BigDecimal maxArea,
            Pageable pageable
    );

    List<FarmSummaryResp> findAllAccessibleFarms(Integer currentUserId);

    FarmResp updateFarm(Integer currentUserId, Integer farmId, UpdateFarmReq request);

    void deleteFarm(Integer currentUserId, Integer farmId);

    FarmMemberResp addMember(Integer currentUserId, Integer farmId, AddFarmMemberReq request);

    List<FarmMemberResp> getFarmMembers(Integer currentUserId, Integer farmId);

    void removeMember(Integer currentUserId, Integer farmId, Integer memberId);

    StaffZoneAssignmentResp assignStaffToZone(Integer currentUserId, Integer farmId, Integer memberId, AssignStaffZoneReq request);

    ZoneResp createZone(Integer currentUserId, Integer farmId, CreateZoneReq request);

    List<ZoneResp> getZones(Integer currentUserId, Integer farmId);
}
