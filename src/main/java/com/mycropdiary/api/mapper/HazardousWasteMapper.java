package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.compliance.HazardousWasteRespDTO;
import com.mycropdiary.api.entity.compliance.HazardousWasteLog;
import org.springframework.stereotype.Component;

@Component
public class HazardousWasteMapper {

    public HazardousWasteRespDTO toResponse(HazardousWasteLog log) {
        return new HazardousWasteRespDTO(
                log.getId(),
                log.getFarm() != null ? log.getFarm().getId() : null,
                log.getWasteType(),
                log.getQuantity(),
                log.getUnit(),
                log.getCollectionDate(),
                log.getDisposalMethod(),
                log.getContractorName(),
                log.getHandoverReceiptNo(),
                log.getCollectorName(),
                log.getNotes(),
                log.getCreatedAt()
        );
    }
}
