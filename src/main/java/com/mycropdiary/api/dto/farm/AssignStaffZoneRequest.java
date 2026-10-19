package com.mycropdiary.api.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * DTO yêu cầu phân công nhân viên vào Vùng canh tác / Zone.
 */
public record AssignStaffZoneRequest(
        @NotNull(message = "Zone ID is required")
        @JsonAlias({"productionAreaId", "zoneId"})
        UUID zoneId,

        Instant startDate,
        Instant endDate
) {
    public AssignStaffZoneRequest(UUID zoneId) {
        this(zoneId, (Instant) null, (Instant) null);
    }

    public UUID productionAreaId() { return zoneId; }
}
