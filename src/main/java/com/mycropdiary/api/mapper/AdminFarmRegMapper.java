package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.farm.AdminFarmRegRespDTO;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.farmregistration.FarmRegistration;
import org.springframework.stereotype.Component;

@Component
public class AdminFarmRegMapper {

    public AdminFarmRegRespDTO toAdminResponse(FarmRegistration reg) {
        if (reg == null) {
            return null;
        }
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

    public AdminFarmRegRespDTO toResponse(FarmRegistration reg) {
        return toAdminResponse(reg);
    }
}
