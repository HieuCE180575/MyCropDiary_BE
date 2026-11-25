package com.mycropdiary.api.dto.compliance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record HazardousWasteReqDTO(
        @NotBlank(message = "Loại rác thải không được để trống")
        String wasteType,

        @NotNull(message = "Số lượng không được để trống")
        @DecimalMin(value = "0.01", message = "Số lượng rác thải phải lớn hơn 0")
        BigDecimal quantity,

        @NotBlank(message = "Đơn vị tính không được để trống")
        String unit,

        @NotNull(message = "Ngày thu gom không được để trống")
        LocalDate collectionDate,

        @NotBlank(message = "Phương pháp xử lý không được để trống")
        String disposalMethod,

        String contractorName,
        String handoverReceiptNo,

        @NotBlank(message = "Người thu gom / ghi nhận không được để trống")
        String collectorName,

        String notes
) {}
