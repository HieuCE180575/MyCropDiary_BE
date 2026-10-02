package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.staff.AddStaffRequest;
import com.mycropdiary.api.dto.staff.FarmMemberResponse;
import com.mycropdiary.api.dto.staff.UpdateStaffRequest;

import java.util.List;

public interface FarmStaffService {
    List<FarmMemberResponse> getStaffList(Long currentUserId, Long farmId, String status, String keyword);
    FarmMemberResponse addStaff(Long currentUserId, Long farmId, AddStaffRequest request);
    FarmMemberResponse updateStaff(Long currentUserId, Long farmId, Long memberId, UpdateStaffRequest request);
    void deactivateStaff(Long currentUserId, Long farmId, Long memberId);
}
