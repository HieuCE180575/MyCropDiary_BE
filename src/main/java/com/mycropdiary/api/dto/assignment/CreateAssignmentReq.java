package com.mycropdiary.api.dto.assignment;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CreateAssignmentReq(
        @NotNull(message = "ID thành viên trang trại (farmMemberId) không được để trống")
        Integer farmMemberId,

        @NotNull(message = "ID khu vực sản xuất (zoneId / productionAreaId) không được để trống")
        @JsonAlias({"productionAreaId", "zoneId"})
        Integer zoneId,

        Instant assignedAt,
        Instant endedAt
) {
    public Integer productionAreaId() { return zoneId; }
}
