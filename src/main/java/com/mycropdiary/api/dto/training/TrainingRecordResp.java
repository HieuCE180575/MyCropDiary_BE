package com.mycropdiary.api.dto.training;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TrainingRecordResp(
        Integer trainingRecordId,
        Integer farmId,
        String farmName,
        Integer organizedByMemberId,
        String organizerName,
        String trainingTitle,
        String description,
        LocalDate trainingDate,
        String imageUrl,
        int attendeeCount,
        List<TrainingAttendeeResp> attendees,
        LocalDateTime createdAt
) {
}
