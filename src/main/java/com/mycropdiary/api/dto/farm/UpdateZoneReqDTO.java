package com.mycropdiary.api.dto.farm;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO yêu cầu cập nhật Vùng canh tác / Zone (UC-16).
 */
public record UpdateZoneReqDTO(
        @NotBlank(message = "Tên vùng không được để trống")
        @Size(max = 120, message = "Tên vùng tối đa 120 ký tự")
        @JsonAlias({"areaName", "name", "zoneName"})
        String zoneName,

        @DecimalMin(value = "0.01", message = "Diện tích phân khu phải lớn hơn 0")
        @JsonAlias({"areaM2", "areaValue"})
        BigDecimal areaValue,
        String areaUnit,
        String description,
        String imageUrl
) {
    public UpdateZoneReqDTO(String zoneName, BigDecimal areaValue, String description) {
        this(zoneName, areaValue, "m2", description, null);
    }
}
