package com.mycropdiary.api.dto.farm;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu thêm thành viên mới vào trang trại.
 */
public record AddFarmMemberReq(
        @NotBlank(message = "User email is required")
        @Email(message = "Must be a valid email format")
        @Size(max = 255, message = "User email must not exceed 255 characters")
        String email,

        @NotBlank(message = "Farm role is required")
        @Pattern(regexp = "^(OWNER|STAFF)$", message = "Farm role must be OWNER or STAFF")
        String farmRole,

        String jobTitle
) {}
