package com.mycropdiary.api.dto.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FarmEquipmentReqDTO(
        @NotBlank(message = "Mã thiết bị không được để trống")
        @Size(max = 60, message = "Mã thiết bị tối đa 60 ký tự")
        String equipmentCode,

        @NotBlank(message = "Tên thiết bị không được để trống")
        @Size(max = 160, message = "Tên thiết bị tối đa 160 ký tự")
        String equipmentName,

        @NotBlank(message = "Loại thiết bị không được để trống")
        String equipmentType,

        String status,
        String notes
) {}
