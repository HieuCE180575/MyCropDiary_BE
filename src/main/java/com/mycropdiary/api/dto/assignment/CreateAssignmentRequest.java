package com.mycropdiary.api.dto.assignment;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record CreateAssignmentRequest(
        @NotNull(message = "ID thành viên trang trại (farmMemberId) không được để trống")
        UUID farmMemberId,

        @NotNull(message = "ID khu vực sản xuất (zoneId / productionAreaId) không được để trống")
        @JsonAlias({"productionAreaId", "zoneId"})
        UUID zoneId,

        Instant assignedAt,
        Instant endedAt
) {
    public UUID productionAreaId() { return zoneId; }
}
