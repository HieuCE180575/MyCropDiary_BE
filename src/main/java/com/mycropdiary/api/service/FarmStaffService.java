package com.mycropdiary.api.service;

import com.mycropdiary.api.dto.staff.CreateStaffReqDTO;
import com.mycropdiary.api.dto.staff.CandidateUserRespDTO;
import com.mycropdiary.api.dto.staff.FarmInvitationRespDTO;
import com.mycropdiary.api.dto.staff.FarmMemberRespDTO;
import com.mycropdiary.api.dto.staff.UpdateStaffReqDTO;

import java.util.List;

public interface FarmStaffService {
    List<FarmMemberRespDTO> getStaffList(Integer currentUserId, Integer farmId, String status, String keyword);
    List<CandidateUserRespDTO> getCandidateUsers(Integer currentUserId, Integer farmId, String keyword);
    FarmMemberRespDTO addStaff(Integer currentUserId, Integer farmId, CreateStaffReqDTO request);
    FarmMemberRespDTO updateStaff(Integer currentUserId, Integer farmId, Integer memberId, UpdateStaffReqDTO request);
    void deactivateStaff(Integer currentUserId, Integer farmId, Integer memberId);
    void deleteStaff(Integer currentUserId, Integer farmId, Integer memberId);
    List<FarmInvitationRespDTO> getPendingInvitations(Integer currentUserId);
    void respondInvitation(Integer currentUserId, Integer memberId, String action);
}
