package com.mycropdiary.api.dto.staff;

public record CandidateUserResp(
        Integer userId,
        String fullName,
        String email,
        String phoneNumber,
        String avatarUrl,
        String systemRole
) {
}

