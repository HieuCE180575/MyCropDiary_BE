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
     * Chuyển đổi Entity Farm sang FarmSummaryResponse DTO (Danh sách tóm tắt).
     */
    public FarmSummaryResponse toSummary(Farm farm, String userRole) {
        return new FarmSummaryResponse(
                farm.getId(),
                farm.getFarmName(),
                farm.getDescription(),
                farm.getAddress(),
                farm.getFarmStatus(),
                farm.getCreatedAt(),
                userRole
        );
    }

    public FarmSummaryResponse toSummary(Farm farm) {
        return toSummary(farm, null);
    }

    /**
     * Chuyển đổi Entity Farm sang FarmResponse DTO (Chi tiết trang trại).
     */
    public FarmResponse toResponse(Farm farm, String userRole) {
        return new FarmResponse(
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
     * Chuyển đổi Entity FarmMember sang FarmMemberResponse DTO (Thông tin thành viên).
     */
    public FarmMemberResponse toMemberResponse(FarmMember member) {
        return new FarmMemberResponse(
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
     * Chuyển đổi Entity StaffZoneAssignment sang StaffZoneAssignmentResponse DTO.
     */
    public StaffZoneAssignmentResponse toStaffZoneAssignmentResponse(StaffZoneAssignment assignment) {
        return new StaffZoneAssignmentResponse(
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
     * Chuyển đổi Entity Zone sang ZoneResponse DTO.
     */
    public ZoneResponse toZoneResponse(Zone zone) {
        return new ZoneResponse(
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
