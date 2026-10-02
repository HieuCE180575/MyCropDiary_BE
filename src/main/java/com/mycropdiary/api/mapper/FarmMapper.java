package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.farm.*;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.ProductionArea;
import com.mycropdiary.api.entity.StaffAreaAssignment;
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
                farm.getFarmCode(),
                farm.getFarmName(),
                farm.getProvince(),
                farm.getDistrict(),
                farm.getTotalAreaM2(),
                farm.getStatus(),
                userRole
        );
    }

    public FarmSummaryResponse toSummary(Farm farm) {
        return new FarmSummaryResponse(
                farm.getId(),
                farm.getFarmCode(),
                farm.getFarmName(),
                farm.getAddressLine(),
                farm.getProvince(),
                farm.getDistrict(),
                farm.getWard(),
                farm.getTotalAreaM2(),
                farm.getStatus(),
                farm.getCreatedAt()
        );
    }

    /**
     * Chuyển đổi Entity Farm sang FarmResponse DTO (Chi tiết trang trại).
     */
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
                member.getFarmRole(),
                member.getJobTitle(),
                member.getJoinedAt(),
                member.getStatus()
        );
    }

    /**
     * Chuyển đổi Entity StaffAreaAssignment sang StaffAreaAssignmentResponse DTO (Phân công nhân viên).
     */
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

    /**
     * Chuyển đổi Entity ProductionArea sang ProductionAreaResponse DTO (Vùng sản xuất).
     */
    public ProductionAreaResponse toProductionAreaResponse(ProductionArea area) {
        return new ProductionAreaResponse(
                area.getId(),
                area.getFarm().getId(),
                area.getAreaCode(),
                area.getAreaName(),
                area.getAreaM2(),
                area.getDescription(),
                area.getStatus(),
                area.getCreatedAt(),
                area.getUpdatedAt()
        );
    }
}
