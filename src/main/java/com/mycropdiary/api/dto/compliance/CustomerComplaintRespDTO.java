package com.mycropdiary.api.dto.compliance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record CustomerComplaintRespDTO(
        Integer complaintId,
        Integer farmId,
        Integer harvestRecordId,
        String lotCode,
        BigDecimal harvestedQuantity,
        String harvestedUnit,
        String customerName,
        String customerPhone,
        LocalDate receivedDate,
        String issueType,
        String issueDescription,
        String severity,
        String status,
        String resolutionNotes,
        Boolean isRecallRequired,
        BigDecimal recalledQuantity,
        Instant createdAt
) {}
