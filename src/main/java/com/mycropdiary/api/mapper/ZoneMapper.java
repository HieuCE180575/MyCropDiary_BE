package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.assignment.AssignmentResp;
import com.mycropdiary.api.dto.farm.ZoneResp;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffZoneAssignment;
import com.mycropdiary.api.entity.Zone;
import org.springframework.stereotype.Component;

/**
 * Lớp chuyển đổi dữ liệu (Mapper) giữa các JPA Entities và DTOs của Module Zone & Zone Assignment.
 */
@Component
public class ZoneMapper {

    public ZoneResp toZoneResponse(Zone zone) {
        return new ZoneResp(
                zone.getId(),
                zone.getFarm().getId(),
                zone.getZoneName(),
                zone.getAreaValue(),
                zone.getAreaUnit(),
                zone.getDescription(),
                zone.getImageUrl(),
                "active",
                zone.getCreatedAt(),
                zone.getCreatedAt()
        );
    }

    public AssignmentResp toAssignmentResponse(StaffZoneAssignment a, FarmMember assigner) {
        FarmMember fm = a.getFarmMember();
        Zone zone = a.getZone();
        return new AssignmentResp(
                a.getId(),
                fm.getId(),
                fm.getUser() != null ? fm.getUser().getId() : null,
                fm.getUser() != null ? fm.getUser().getFullName() : null,
                fm.getUser() != null ? fm.getUser().getEmail() : null,
                fm.getJobTitle(),
                zone.getId(),
                zone.getZoneName(),
                assigner != null ? assigner.getId() : null,
                assigner != null && assigner.getUser() != null ? assigner.getUser().getFullName() : null,
                a.getAssignedAt(),
                a.getEndedAt(),
                a.isActive()
        );
    }
}
