package com.mycropdiary.api.dto.training;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CreateTrainingRecordReq(
        @NotBlank(message = "Chủ đề / tiêu đề đào tạo không được để trống")
        @Size(max = 180, message = "Tiêu đề đào tạo tối đa 180 ký tự")
        String trainingTitle,

        String description,

        @NotNull(message = "Ngày đào tạo không được để trống")
        LocalDate trainingDate,

        String imageUrl,

        Integer organizedByMemberId,

        @Valid
        List<TrainingAttendeeReq> attendees
) {
}
