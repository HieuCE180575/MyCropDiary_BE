package com.mycropdiary.api.dto.cultivation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO chứa thông tin nhân công trực tiếp thực hiện một nhật ký canh tác.
 */
public record ActivityWorkerRequest(
        @NotNull(message = "Farm worker ID is required")
        Long workerId,

        @PositiveOrZero(message = "Work hours must be positive or zero")
        BigDecimal workHours,

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        String notes
) {}
