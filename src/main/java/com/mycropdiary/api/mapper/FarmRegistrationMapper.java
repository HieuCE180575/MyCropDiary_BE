package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.farm.FarmRegistrationResp;
import com.mycropdiary.api.dto.farmregistration.FarmRegistrationSummaryResp;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.farmregistration.FarmRegistration;
import org.springframework.stereotype.Component;

/**
 * Lớp chuyển đổi dữ liệu (Mapper) giữa Entity FarmRegistration và các DTOs (Admin & User).
 */
@Component
public class FarmRegistrationMapper {

    public FarmRegistrationResp toAdminResponse(FarmRegistration reg) {
        User applicant = reg.getUser();
        User handler = reg.getReviewedBy();

        return new FarmRegistrationResp(
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

    public com.mycropdiary.api.dto.farmregistration.FarmRegistrationResp toUserResponse(FarmRegistration reg) {
        return new com.mycropdiary.api.dto.farmregistration.FarmRegistrationResp(
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

    public FarmRegistrationSummaryResp toSummaryResponse(FarmRegistration reg) {
        return new FarmRegistrationSummaryResp(
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
