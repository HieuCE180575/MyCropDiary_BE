package com.mycropdiary.api.dto.compliance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record HazardousWasteRespDTO(
        Integer wasteLogId,
        Integer farmId,
        String wasteType,
        BigDecimal quantity,
        String unit,
        LocalDate collectionDate,
        String disposalMethod,
        String contractorName,
        String handoverReceiptNo,
        String collectorName,
        String notes,
        Instant createdAt
) {}
