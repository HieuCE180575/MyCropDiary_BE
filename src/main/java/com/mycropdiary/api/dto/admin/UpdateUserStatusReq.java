package com.mycropdiary.api.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateUserStatusReq(
        @NotBlank(message = "Trạng thái tài khoản không được để trống")
        @Pattern(regexp = "(?i)^(ACTIVE|LOCKED|PENDING)$", message = "Trạng thái chỉ có thể là ACTIVE, LOCKED hoặc PENDING")
        String status
) {}
