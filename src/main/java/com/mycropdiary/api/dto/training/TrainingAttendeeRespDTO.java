package com.mycropdiary.api.dto.training;

public record TrainingAttendeeRespDTO(
        Integer farmWorkerId,
        String workerCode,
        String fullName,
        String jobTitle,
        String attendanceStatus,
        String notes
) {
}

