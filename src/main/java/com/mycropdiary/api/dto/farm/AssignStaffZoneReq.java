package com.mycropdiary.api.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * DTO yêu cầu phân công nhân viên vào Vùng canh tác / Zone.
 */
public record AssignStaffZoneReq(
        @NotNull(message = "Zone ID is required")
        @JsonAlias({"productionAreaId", "zoneId"})
        Integer zoneId,

        Instant startDate,
        Instant endDate
) {
    public AssignStaffZoneReq(Integer zoneId) {
        this(zoneId, (Instant) null, (Instant) null);
    }

    public Integer productionAreaId() { return zoneId; }
}
