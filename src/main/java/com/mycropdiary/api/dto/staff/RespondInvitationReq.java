package com.mycropdiary.api.dto.staff;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RespondInvitationReq(
        @NotNull(message = "Hành động là bắt buộc")
        @Pattern(regexp = "^(?i)(approve|reject)$", message = "Hành động phải là approve hoặc reject")
        String action
) {
}

