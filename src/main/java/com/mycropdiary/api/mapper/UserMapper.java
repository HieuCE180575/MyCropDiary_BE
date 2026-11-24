package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.admin.UserRespDTO;
import com.mycropdiary.api.dto.user.UserProfileRespDTO;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffZoneAssignment;
import com.mycropdiary.api.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    public UserProfileRespDTO toProfileResponse(User user) {
        return UserProfileRespDTO.fromEntity(user);
    }

    public UserRespDTO toResponse(User user) {
        return UserRespDTO.fromEntity(user);
    }

    public UserRespDTO toEnrichedUserResp(User user, List<FarmMember> memberships, List<StaffZoneAssignment> zoneAssignments) {
        String farmsSummary = memberships.isEmpty()
                ? "Chưa tham gia trang trại"
                : memberships.stream()
                        .filter(m -> m.getFarm() != null)
                        .map(m -> {
                            String roleLabel = m.isOwner() ? "Chủ trại" : "Nhân sự";
                            return m.getFarm().getFarmName() + " (" + roleLabel + ")";
                        })
                        .collect(Collectors.joining(", "));

        String zonesSummary = zoneAssignments.isEmpty()
                ? "Chưa phụ trách phân khu"
                : zoneAssignments.stream()
                        .filter(a -> a.getZone() != null)
                        .map(a -> a.getZone().getZoneName() + (a.getZone().getFarm() != null ? " - " + a.getZone().getFarm().getFarmName() : ""))
                        .collect(Collectors.joining(", "));

        return UserRespDTO.fromEntity(user, farmsSummary, zonesSummary);
    }
}
