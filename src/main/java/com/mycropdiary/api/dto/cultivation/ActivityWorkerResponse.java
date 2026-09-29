package com.mycropdiary.api.dto.cultivation;

import java.math.BigDecimal;

/**
 * DTO phản hồi thông tin nhân công trực tiếp thực hiện trong nhật ký canh tác.
 */
public record ActivityWorkerResponse(
        Long workerId,
        String workerCode,
        String fullName,
        BigDecimal workHours,
        String notes
) {}
