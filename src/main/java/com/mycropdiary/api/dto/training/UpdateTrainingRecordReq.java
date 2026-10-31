package com.mycropdiary.api.dto.training;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record UpdateTrainingRecordReq(
        @Size(max = 180, message = "Tiêu đề đào tạo tối đa 180 ký tự")
        String trainingTitle,

        String description,

        LocalDate trainingDate,

        String imageUrl,

        Integer organizedByMemberId,

        @Valid
        List<TrainingAttendeeReq> attendees
) {
}
