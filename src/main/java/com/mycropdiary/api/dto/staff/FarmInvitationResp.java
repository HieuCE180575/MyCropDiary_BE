package com.mycropdiary.api.dto.staff;

import java.time.Instant;

public record FarmInvitationResp(
        Integer memberId,
        Integer farmId,
        String farmName,
        String farmAddress,
        String ownerName,
        String farmRole,
        String jobTitle,
        Instant invitedAt
) {
}

