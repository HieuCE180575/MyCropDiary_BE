package com.mycropdiary.api.dto.cultivation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateCropSeasonStatusReq(
        @NotBlank(message = "Trạng thái mùa vụ không được để trống")
        @Pattern(regexp = "(?i)^(planned|active|completed|cancelled)$", message = "Trạng thái mùa vụ phải là planned, active, completed hoặc cancelled")
        String status
) {
}

