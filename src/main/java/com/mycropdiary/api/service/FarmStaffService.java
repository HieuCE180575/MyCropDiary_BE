package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.staff.AddStaffReq;
import com.mycropdiary.api.dto.staff.CandidateUserResp;
import com.mycropdiary.api.dto.staff.FarmInvitationResp;
import com.mycropdiary.api.dto.staff.FarmMemberResp;
import com.mycropdiary.api.dto.staff.UpdateStaffReq;

import java.util.List;

public interface FarmStaffService {
    List<FarmMemberResp> getStaffList(Integer currentUserId, Integer farmId, String status, String keyword);
    List<CandidateUserResp> getCandidateUsers(Integer currentUserId, Integer farmId, String keyword);
    FarmMemberResp addStaff(Integer currentUserId, Integer farmId, AddStaffReq request);
    FarmMemberResp updateStaff(Integer currentUserId, Integer farmId, Integer memberId, UpdateStaffReq request);
    void deactivateStaff(Integer currentUserId, Integer farmId, Integer memberId);
    void deleteStaff(Integer currentUserId, Integer farmId, Integer memberId);
    List<FarmInvitationResp> getPendingInvitations(Integer currentUserId);
    void respondInvitation(Integer currentUserId, Integer memberId, String action);
}
