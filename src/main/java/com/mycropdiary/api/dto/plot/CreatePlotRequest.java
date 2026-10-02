package com.mycropdiary.api.dto.plot;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreatePlotRequest(
        @NotBlank(message = "Mã thửa đất không được để trống")
        @Size(max = 30, message = "Mã thửa tối đa 30 ký tự")
        String plotCode,

        @NotBlank(message = "Tên thửa đất không được để trống")
        @Size(max = 150, message = "Tên thửa tối đa 150 ký tự")
        String plotName,

        @NotNull(message = "Diện tích thửa đất không được để trống")
        @DecimalMin(value = "0.01", message = "Diện tích thửa đất phải lớn hơn 0")
        BigDecimal areaM2,

        @DecimalMin(value = "-90.0", message = "Vĩ độ không hợp lệ (-90 đến 90)")
        @DecimalMax(value = "90.0", message = "Vĩ độ không hợp lệ (-90 đến 90)")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0", message = "Kinh độ không hợp lệ (-180 đến 180)")
        @DecimalMax(value = "180.0", message = "Kinh độ không hợp lệ (-180 đến 180)")
        BigDecimal longitude,

        String boundaryGeoJson,

        @Pattern(regexp = "AVAILABLE|IN_USE|RESTING|INACTIVE", message = "Trạng thái không hợp lệ (AVAILABLE, IN_USE, RESTING, INACTIVE)")
        String status
) {
}
