package com.mycropdiary.api.dto.training;

public record TrainingAttendeeResp(
        Integer farmWorkerId,
        String workerCode,
        String fullName,
        String jobTitle,
        String attendanceStatus,
        String notes
) {
}

