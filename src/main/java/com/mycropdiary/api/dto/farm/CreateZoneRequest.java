package com.mycropdiary.api.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO yêu cầu tạo mới Vùng canh tác / Zone.
 */
public record CreateZoneRequest(
        @NotBlank(message = "Tên vùng không được để trống")
        @Size(max = 120, message = "Tên vùng tối đa 120 ký tự")
        @JsonAlias({"areaName", "name", "zoneName"})
        String zoneName,

        @JsonAlias({"areaM2", "areaValue"})
        BigDecimal areaValue,
        String areaUnit,
        String description,
        String imageUrl
) {
    public CreateZoneRequest(String areaCode, String areaName, BigDecimal areaM2, String description) {
        this(areaName != null && !areaName.isBlank() ? areaName : areaCode, areaM2, "m2", description, null);
    }

    public CreateZoneRequest(String zoneName, BigDecimal areaValue, String description) {
        this(zoneName, areaValue, "m2", description, null);
    }

    public String areaName() { return zoneName; }
    public String areaCode() { return zoneName; }
    public BigDecimal areaM2() { return areaValue; }
}
