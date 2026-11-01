package com.mycropdiary.api.dto.soil;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Yêu cầu cập nhật đánh giá thổ nhưỡng lô đất (Update Soil Assessment)")
public record UpdateSoilAssessmentReq(
        @Schema(description = "Độ pH của đất (thang đo 0.0 - 14.0)", example = "6.8")
        @DecimalMin(value = "0.0", message = "pH đất tối thiểu là 0.0")
        @DecimalMax(value = "14.0", message = "pH đất tối đa là 14.0")
        BigDecimal soilPh,

        @Schema(description = "Độ ẩm của đất (%)", example = "60.0")
        @DecimalMin(value = "0.0", message = "Độ ẩm tối thiểu là 0%")
        @DecimalMax(value = "100.0", message = "Độ ẩm tối đa là 100%")
        BigDecimal moisturePercent,

        @Schema(description = "Dữ liệu phân tích dinh dưỡng hoặc hàm lượng mùn (JSON / text)", example = "{\"organicMatterPct\": 3.8, \"nitrogen\": \"OPTIMAL\"}")
        String nutrientData,

        @Schema(description = "Ghi chú & kết luận đánh giá", example = "Đất sau cải tạo đạt chuẩn VietGAP")
        String notes,

        @Schema(description = "Ảnh phiếu kết quả kiểm nghiệm hoặc mẫu đất")
        String imageUrl,

        @Schema(description = "Thời điểm lấy mẫu / đánh giá")
        Instant assessedAt
) {
}

