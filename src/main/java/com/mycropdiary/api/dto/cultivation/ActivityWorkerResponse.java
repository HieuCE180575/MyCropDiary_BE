package com.mycropdiary.api.dto.cultivation;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO phản hồi thông tin nhân công trực tiếp thực hiện trong nhật ký canh tác.
 */
public record ActivityWorkerResponse(
        UUID workerId,
        String workerCode,
        String fullName,
        BigDecimal workHours,
        String notes
) {}
