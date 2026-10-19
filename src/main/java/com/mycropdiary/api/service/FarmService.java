package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.farm.*;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Interface định nghĩa các dịch vụ nghiệp vụ cốt lõi cho trang trại (Farm Core).
 */
public interface FarmService {
    FarmResponse createFarm(UUID currentUserId, CreateFarmRequest request);

    FarmResponse getFarmById(UUID currentUserId, UUID farmId);

    PageResponse<FarmSummaryResponse> searchFarms(UUID currentUserId, String keyword, String status, String province, Pageable pageable);

    PageResponse<FarmSummaryResponse> searchFarms(
            UUID currentUserId,
            String keyword,
            String province,
            String status,
            BigDecimal minArea,
            BigDecimal maxArea,
            Pageable pageable
    );

    List<FarmSummaryResponse> findAllAccessibleFarms(UUID currentUserId);

    FarmResponse updateFarm(UUID currentUserId, UUID farmId, UpdateFarmRequest request);

    void deleteFarm(UUID currentUserId, UUID farmId);

    FarmMemberResponse addMember(UUID currentUserId, UUID farmId, AddFarmMemberRequest request);

    List<FarmMemberResponse> getFarmMembers(UUID currentUserId, UUID farmId);

    void removeMember(UUID currentUserId, UUID farmId, UUID memberId);

    StaffZoneAssignmentResponse assignStaffToZone(UUID currentUserId, UUID farmId, UUID memberId, AssignStaffZoneRequest request);

    ZoneResponse createZone(UUID currentUserId, UUID farmId, CreateZoneRequest request);

    List<ZoneResponse> getZones(UUID currentUserId, UUID farmId);
}
