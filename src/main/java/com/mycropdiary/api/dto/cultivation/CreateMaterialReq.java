package com.mycropdiary.api.dto.cultivation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMaterialReq(
        @NotNull(message = "Farm ID is required")
        Integer farmId,

        @NotBlank(message = "Tên vật tư không được để trống")
        @Size(max = 160, message = "Tên vật tư tối đa 160 ký tự")
        String materialName,

        @Size(max = 80, message = "Phân loại vật tư tối đa 80 ký tự")
        String materialType,

        @NotBlank(message = "Đơn vị tính không được để trống")
        @Size(max = 30, message = "Đơn vị tính tối đa 30 ký tự")
        String unit,

        String description
) {
}
