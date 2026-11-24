package com.mycropdiary.api.dto.admin;

public record RegistrationStatsRespDTO(
        long totalRegistrations,
        long pendingRegistrations,
        long approvedRegistrations,
        long rejectedRegistrations
) {}
