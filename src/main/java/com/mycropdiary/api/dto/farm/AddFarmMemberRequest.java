package com.mycropdiary.api.dto.farm;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddFarmMemberRequest(
        @NotBlank(message = "User email is required")
        @Email(message = "Must be a valid email format")
        String email,

        @NotBlank(message = "Farm role is required")
        @Pattern(regexp = "^(OWNER|STAFF)$", message = "Farm role must be OWNER or STAFF")
        String farmRole,

        String jobTitle
) {}
