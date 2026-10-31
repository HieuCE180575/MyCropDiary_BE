package com.mycropdiary.api.dto.vietgap;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateVietGapCriterionReq(
        @Size(max = 160, message = "Tên tiêu chuẩn tối đa 160 ký tự")
        String criterionName,

        String description,

        @DecimalMin(value = "0.01", message = "Thang điểm tối đa phải lớn hơn 0")
        BigDecimal maxScore,

        String evaluationMethod,

        Boolean isActive
) {
}
