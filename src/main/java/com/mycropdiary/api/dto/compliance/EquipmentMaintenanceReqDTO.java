package com.mycropdiary.api.dto.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record EquipmentMaintenanceReqDTO(
        Integer equipmentId,
        String equipmentCode,
        String equipmentName,

        @NotBlank(message = "Loại bảo dưỡng / vệ sinh không được để trống")
        String actionType,

        @NotNull(message = "Ngày thực hiện không được để trống")
        LocalDate actionDate,

        @NotBlank(message = "Người thực hiện không được để trống")
        String performerName,

        @NotBlank(message = "Cách xử lý nước rửa không được để trống")
        String rinseWaterHandling,

        Boolean isSafeForUse,
        String notes
) {}
