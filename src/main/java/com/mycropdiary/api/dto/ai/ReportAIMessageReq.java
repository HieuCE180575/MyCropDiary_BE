package com.mycropdiary.api.dto.ai;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ReportAIMessageReq(
        @Min(value = 1, message = "Đánh giá tối thiểu 1 sao")
        @Max(value = 5, message = "Đánh giá tối đa 5 sao")
        Integer rating,

        @NotBlank(message = "Lý do báo cáo / nội dung phản hồi không được để trống")
        String reason
) {
}

