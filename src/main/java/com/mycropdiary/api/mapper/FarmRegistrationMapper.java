package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.farm.AdminFarmRegRespDTO;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationSummaryRespDTO;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.farmregistration.FarmRegistration;
import org.springframework.stereotype.Component;

/**
 * Lớp chuyển đổi dữ liệu (Mapper) giữa Entity FarmRegistration và các DTOs (Admin & User).
 */
@Component
public class FarmRegistrationMapper {

    public AdminFarmRegRespDTO toAdminResponse(FarmRegistration reg) {
        User applicant = reg.getUser();
        User handler = reg.getReviewedBy();

        return new AdminFarmRegRespDTO(
                reg.getId(),
                applicant != null ? applicant.getId() : null,
                applicant != null ? applicant.getEmail() : null,
                applicant != null ? applicant.getFullName() : null,
                reg.getFarmName(),
                reg.getAddress(),
                reg.getProvince(),
                reg.getDistrict(),
                reg.getWard(),
                reg.getDescription(),
                reg.getDocumentUrl(),
                reg.getRegistrationStatus() != null ? reg.getRegistrationStatus().toUpperCase() : null,
                handler != null ? handler.getId() : null,
                handler != null ? handler.getFullName() : null,
                reg.getSubmittedAt(),
                reg.getHandledAt(),
                reg.getRejectionReason(),
                reg.getLatitude(),
                reg.getLongitude(),
                reg.getBoundaryCoordinates()
        );
    }

    public com.mycropdiary.api.dto.farmregistration.FarmRegistrationRespDTO toUserResponse(FarmRegistration reg) {
        return new com.mycropdiary.api.dto.farmregistration.FarmRegistrationRespDTO(
                reg.getId(),
                reg.getFarmName(),
                reg.getAddress(),
                reg.getProvince(),
                reg.getDistrict(),
                reg.getWard(),
                reg.getDescription(),
                reg.getDocumentUrl(),
                reg.getStatus(),
                reg.getSubmittedAt(),
                reg.getHandledAt(),
                reg.getRejectionReason(),
                reg.getLatitude(),
                reg.getLongitude(),
                reg.getBoundaryCoordinates()
        );
    }

    public FarmRegistrationSummaryRespDTO toSummaryResponse(FarmRegistration reg) {
        return new FarmRegistrationSummaryRespDTO(
                reg.getId(),
                reg.getFarmName(),
                reg.getAddress(),
                reg.getProvince(),
                reg.getDistrict(),
                reg.getWard(),
                reg.getStatus(),
                reg.getSubmittedAt(),
                reg.getHandledAt(),
                reg.getRejectionReason(),
                reg.getLatitude(),
                reg.getLongitude(),
                reg.getBoundaryCoordinates()
        );
    }
}
