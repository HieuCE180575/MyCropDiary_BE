package com.mycropdiary.api.dto.compliance;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RecallProductReqDTO(
        @NotNull(message = "Số lượng thu hồi không được để trống")
        @DecimalMin(value = "0.01", message = "Số lượng thu hồi phải lớn hơn 0")
        BigDecimal recalledQuantity,

        @NotBlank(message = "Ghi chú quy trình thu hồi & xử lý không được để trống")
        String resolutionNotes
) {}
