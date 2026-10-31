package com.mycropdiary.api.dto.vietgap;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateVietGapCriterionReq(
        @NotBlank(message = "Tên tiêu chuẩn không được để trống")
        @Size(max = 160, message = "Tên tiêu chuẩn tối đa 160 ký tự")
        String criterionName,

        String description,

        @DecimalMin(value = "0.01", message = "Thang điểm tối đa phải lớn hơn 0")
        BigDecimal maxScore,

        String evaluationMethod,

        String standardReference, // Mặc định: "TCVN 11892-1:2017"

        Boolean isActive
) {
    public CreateVietGapCriterionReq(String criterionName, String description, BigDecimal maxScore, String evaluationMethod) {
        this(criterionName, description, maxScore, evaluationMethod, "TCVN 11892-1:2017", true);
    }
}

