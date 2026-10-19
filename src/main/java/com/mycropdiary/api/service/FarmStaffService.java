package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.staff.AddStaffRequest;
import com.mycropdiary.api.dto.staff.FarmMemberResponse;
import com.mycropdiary.api.dto.staff.UpdateStaffRequest;

import java.util.List;
import java.util.UUID;

public interface FarmStaffService {
    List<FarmMemberResponse> getStaffList(UUID currentUserId, UUID farmId, String status, String keyword);
    FarmMemberResponse addStaff(UUID currentUserId, UUID farmId, AddStaffRequest request);
    FarmMemberResponse updateStaff(UUID currentUserId, UUID farmId, UUID memberId, UpdateStaffRequest request);
    void deactivateStaff(UUID currentUserId, UUID farmId, UUID memberId);
}
