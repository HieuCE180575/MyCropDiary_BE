package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.farm.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FarmService {
    FarmResponse createFarm(Long currentUserId, CreateFarmRequest request);
    FarmResponse getFarmById(Long currentUserId, Long farmId);
    PageResponse<FarmSummaryResponse> searchFarms(Long currentUserId, String keyword, String status, String province, Pageable pageable);
    List<FarmSummaryResponse> findAllAccessibleFarms(Long currentUserId);
    FarmResponse updateFarm(Long currentUserId, Long farmId, UpdateFarmRequest request);
    void deleteFarm(Long currentUserId, Long farmId);

    FarmMemberResponse addMember(Long currentUserId, Long farmId, AddFarmMemberRequest request);
    List<FarmMemberResponse> getFarmMembers(Long currentUserId, Long farmId);
    void removeMember(Long currentUserId, Long farmId, Long memberId);
    StaffAreaAssignmentResponse assignStaffToArea(Long currentUserId, Long farmId, Long memberId, AssignStaffAreaRequest request);

    ProductionAreaResponse createProductionArea(Long currentUserId, Long farmId, CreateProductionAreaRequest request);
    List<ProductionAreaResponse> getProductionAreas(Long currentUserId, Long farmId);
}
