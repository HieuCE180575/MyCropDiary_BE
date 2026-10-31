package com.mycropdiary.api.dto.training;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrainingAttendeeReq(
        @NotNull(message = "farmWorkerId không được để trống")
        Integer farmWorkerId,

        @Size(max = 20, message = "Trạng thái điểm danh tối đa 20 ký tự")
        String attendanceStatus, // attended, absent, excused

        String notes
) {
}
