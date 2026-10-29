package com.mycropdiary.api.dto.cultivation;

public record TaskWorkerResp(
        Integer workerId,
        String workerCode,
        String fullName,
        String phoneNumber,
        String jobTitle,
        String assignmentStatus
) {}
