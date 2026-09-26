package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.farm.FarmMemberResponse;
import com.mycropdiary.api.dto.farm.FarmResponse;
import com.mycropdiary.api.dto.farm.FarmSummaryResponse;
import com.mycropdiary.api.dto.farm.StaffAreaAssignmentResponse;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffAreaAssignment;
import org.springframework.stereotype.Component;

@Component
public class FarmMapper {
    public FarmSummaryResponse toSummary(Farm farm, String userRole) {
        return new FarmSummaryResponse(
                farm.getId(),
                farm.getFarmCode(),
                farm.getFarmName(),
                farm.getProvince(),
                farm.getDistrict(),
                farm.getTotalAreaM2(),
                farm.getStatus(),
                userRole
        );
    }

    public FarmResponse toResponse(Farm farm, String userRole) {
        return new FarmResponse(
                farm.getId(),
                farm.getRegistrationId(),
                farm.getFarmCode(),
                farm.getFarmName(),
                farm.getAddressLine(),
                farm.getProvince(),
                farm.getDistrict(),
                farm.getWard(),
                farm.getLatitude(),
                farm.getLongitude(),
                farm.getTotalAreaM2(),
                farm.getStatus(),
                farm.getCreatedAt(),
                farm.getUpdatedAt(),
                userRole
        );
    }

    public FarmMemberResponse toMemberResponse(FarmMember member) {
        return new FarmMemberResponse(
                member.getId(),
                member.getFarm().getId(),
                member.getUser().getId(),
                member.getUser().getFullName(),
                member.getUser().getEmail(),
                member.getFarmRole(),
                member.getJobTitle(),
                member.getJoinedAt(),
                member.getStatus()
        );
    }

    public StaffAreaAssignmentResponse toAssignmentResponse(StaffAreaAssignment assignment) {
        return new StaffAreaAssignmentResponse(
                assignment.getId(),
                assignment.getFarmMember().getId(),
                assignment.getProductionArea().getId(),
                assignment.getProductionArea().getAreaCode(),
                assignment.getProductionArea().getAreaName(),
                assignment.getAssignedByMember() != null ? assignment.getAssignedByMember().getId() : null,
                assignment.getStartDate(),
                assignment.getEndDate(),
                assignment.isActive()
        );
    }
}
