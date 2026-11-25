package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.compliance.CustomerComplaintRespDTO;
import com.mycropdiary.api.entity.compliance.CustomerComplaintLog;
import com.mycropdiary.api.entity.cultivation.HarvestRecord;
import org.springframework.stereotype.Component;

@Component
public class CustomerComplaintMapper {

    public CustomerComplaintRespDTO toResponse(CustomerComplaintLog log) {
        HarvestRecord hr = log.getHarvestRecord();
        return new CustomerComplaintRespDTO(
                log.getId(),
                log.getFarm() != null ? log.getFarm().getId() : null,
                hr != null ? hr.getId() : null,
                log.getLotCode() != null ? log.getLotCode() : (hr != null ? hr.getHarvestLotCode() : null),
                hr != null ? hr.getQuantity() : null,
                hr != null ? hr.getUnit() : null,
                log.getCustomerName(),
                log.getCustomerPhone(),
                log.getReceivedDate(),
                log.getIssueType(),
                log.getIssueDescription(),
                log.getSeverity(),
                log.getStatus(),
                log.getResolutionNotes(),
                log.getIsRecallRequired(),
                log.getRecalledQuantity(),
                log.getCreatedAt()
        );
    }
}
