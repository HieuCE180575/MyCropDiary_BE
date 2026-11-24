package com.mycropdiary.api.dto.staff;

public record CandidateUserRespDTO(
        Integer userId,
        String fullName,
        String email,
        String phoneNumber,
        String avatarUrl,
        String systemRole
) {
}

