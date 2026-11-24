package com.mycropdiary.api.dto.cultivation;

public record TaskWorkerRespDTO(
        Integer workerId,
        String workerCode,
        String fullName,
        String phoneNumber,
        String jobTitle,
        String assignmentStatus
) {}

