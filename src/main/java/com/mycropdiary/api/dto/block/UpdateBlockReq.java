package com.mycropdiary.api.dto.block;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO yêu cầu cập nhật thông tin lô / thửa đất (Block).
 */
public record UpdateBlockReq(
        @NotBlank(message = "Tên lô đất không được để trống")
        @Size(max = 120, message = "Tên lô tối đa 120 ký tự")
        String blockName,

        String description,

        @NotNull(message = "Diện tích lô đất không được để trống")
        @DecimalMin(value = "0.01", message = "Diện tích lô đất phải lớn hơn 0")
        BigDecimal areaValue,

        String areaUnit,
        String soilType,
        String imageUrl,
        String status,
        BigDecimal latitude,
        BigDecimal longitude,
        String boundaryGeoJson
) {
    public UpdateBlockReq(String plotCode, String plotName, BigDecimal areaM2, String areaUnit, String soilType, String description, String imageUrl, BigDecimal latitude, BigDecimal longitude, String boundaryGeoJson, String status) {
        this(plotName != null && !plotName.isBlank() ? plotName : plotCode, description, areaM2, areaUnit, soilType, imageUrl, status, latitude, longitude, boundaryGeoJson);
    }

    public String plotName() { return blockName; }
    public String plotCode() { return blockName; }
    public BigDecimal areaM2() { return areaValue; }
}
