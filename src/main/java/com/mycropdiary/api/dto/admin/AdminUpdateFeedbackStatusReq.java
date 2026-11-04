package com.mycropdiary.api.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminUpdateFeedbackStatusReq(
        @NotBlank(message = "Trạng thái không được để trống")
        String status
) {}

