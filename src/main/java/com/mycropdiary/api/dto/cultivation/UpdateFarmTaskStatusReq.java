package com.mycropdiary.api.dto.cultivation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFarmTaskStatusReq(
        @NotBlank(message = "Trạng thái công việc không được để trống")
        @Size(max = 20, message = "Trạng thái tối đa 20 ký tự")
        String status // planned, in_progress, completed, cancelled
) {}

