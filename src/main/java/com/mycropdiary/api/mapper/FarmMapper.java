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
    public FarmSummaryRespDTO toSummary(Farm farm, String userRole) {
        return new FarmSummaryRespDTO(
                farm.getId(),
                farm.getFarmName(),
                farm.getDescription(),
                farm.getAddress(),
                farm.getFarmStatus(),
                farm.getCreatedAt(),
                userRole
        );
    }

    public FarmSummaryRespDTO toSummary(Farm farm) {
        return toSummary(farm, null);
    }

    /**
     * Chuyển đổi Entity Farm sang FarmResp DTO (Chi tiết trang trại).
     */
    public FarmRespDTO toResponse(Farm farm, String userRole) {
        return new FarmRespDTO(
                farm.getId(),
                farm.getFarmName(),
                farm.getDescription(),
                farm.getAddress(),
                farm.getLatitude(),
                farm.getLongitude(),
                farm.getBoundaryCoordinates(),
                farm.getCoverImageUrl(),
                farm.getFarmMapUrl(),
                farm.getFarmStatus(),
                farm.getCreatedAt(),
                farm.getUpdatedAt(),
                userRole
        );
    }

    /**
     * Chuyển đổi Entity FarmMember sang FarmMemberResp DTO (Thông tin thành viên).
     */
    public FarmMemberRespDTO toMemberResponse(FarmMember member) {
        return new FarmMemberRespDTO(
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
    public StaffZoneAssignmentRespDTO toStaffZoneAssignmentResponse(StaffZoneAssignment assignment) {
        return new StaffZoneAssignmentRespDTO(
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
    public ZoneRespDTO toZoneResponse(Zone zone) {
        return new ZoneRespDTO(
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
