package com.mycropdiary.api.dto.vietgap;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Yêu cầu đánh giá tiêu chí nội bộ")
public record InternalAssessmentItemReq(
        @Schema(description = "ID tiêu chuẩn đánh giá (AssessmentCriterion ID)", example = "1")
        @NotNull(message = "assessmentCriterionId không được để trống")
        Integer assessmentCriterionId,

        @Schema(description = "Điểm số đạt được", example = "95.50")
        @NotNull(message = "Điểm số không được để trống")
        @DecimalMin(value = "0.0", message = "Điểm số không được nhỏ hơn 0")
        BigDecimal score,

        @Schema(description = "Ghi chú kết quả đánh giá", example = "Sổ nhật ký canh tác ghi chép đầy đủ vật tư và thời gian cách ly")
        String resultNotes,

        @Schema(description = "Đường dẫn ảnh bằng chứng thẩm định")
        String evidenceImageUrl
) {
}

