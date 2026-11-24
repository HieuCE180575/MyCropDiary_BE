package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.staff.FarmMemberRespDTO;
import com.mycropdiary.api.entity.FarmMember;
import org.springframework.stereotype.Component;

@Component
public class FarmStaffMapper {

    public FarmMemberRespDTO toStaffMemberResponse(FarmMember member, int activeAssignmentCount) {
        if (member == null) {
            return null;
        }
        return new FarmMemberRespDTO(
                member.getId(),
                member.getFarm().getId(),
                member.getFarm().getFarmName(),
                member.getUser().getId(),
                member.getUser().getFullName(),
                member.getUser().getEmail(),
                member.getUser().getPhone(),
                member.getMemberRole(),
                member.getJobTitle(),
                member.getJoinedAt(),
                null,
                member.getMembershipStatus(),
                activeAssignmentCount
        );
    }
}
