package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.farm.*;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffZoneAssignment;
import com.mycropdiary.api.entity.Zone;
import org.springframework.stereotype.Component;

/**
 * Lớp chuyển đổi dữ liệu (Mapper) giữa các JPA Entities và DTOs của Module Farm Core.
 */
@Component
public class FarmMapper {
    /**
     * Chuyển đổi Entity Farm sang FarmSummaryResp DTO (Danh sách tóm tắt).
     */
    public FarmSummaryResp toSummary(Farm farm, String userRole) {
        return new FarmSummaryResp(
                farm.getId(),
                farm.getFarmName(),
                farm.getDescription(),
                farm.getAddress(),
                farm.getFarmStatus(),
                farm.getCreatedAt(),
                userRole
        );
    }

    public FarmSummaryResp toSummary(Farm farm) {
        return toSummary(farm, null);
    }

    /**
     * Chuyển đổi Entity Farm sang FarmResp DTO (Chi tiết trang trại).
     */
    public FarmResp toResponse(Farm farm, String userRole) {
        return new FarmResp(
                farm.getId(),
                farm.getFarmName(),
                farm.getDescription(),
                farm.getAddress(),
                farm.getLatitude(),
                farm.getLongitude(),
                farm.getCoverImageUrl(),
                farm.getFarmStatus(),
                farm.getCreatedAt(),
                farm.getUpdatedAt(),
                userRole
        );
    }

    /**
     * Chuyển đổi Entity FarmMember sang FarmMemberResp DTO (Thông tin thành viên).
     */
    public FarmMemberResp toMemberResponse(FarmMember member) {
        return new FarmMemberResp(
                member.getId(),
                member.getFarm().getId(),
                member.getUser().getId(),
                member.getUser().getFullName(),
                member.getUser().getEmail(),
                member.getMemberRole(),
                member.getJobTitle(),
                member.getJoinedAt(),
                member.getMembershipStatus()
        );
    }

    /**
     * Chuyển đổi Entity StaffZoneAssignment sang StaffZoneAssignmentResp DTO.
     */
    public StaffZoneAssignmentResp toStaffZoneAssignmentResponse(StaffZoneAssignment assignment) {
        return new StaffZoneAssignmentResp(
                assignment.getId(),
                assignment.getFarmMember().getId(),
                assignment.getZone().getId(),
                assignment.getZone().getZoneName(),
                null,
                assignment.getAssignedAt(),
                assignment.getEndedAt(),
                assignment.isActive()
        );
    }

    /**
     * Chuyển đổi Entity Zone sang ZoneResp DTO.
     */
    public ZoneResp toZoneResponse(Zone zone) {
        return new ZoneResp(
                zone.getId(),
                zone.getFarm().getId(),
                zone.getZoneName(),
                zone.getAreaValue(),
                zone.getAreaUnit(),
                zone.getDescription(),
                zone.getImageUrl(),
                zone.getCreatedAt()
        );
    }
}
