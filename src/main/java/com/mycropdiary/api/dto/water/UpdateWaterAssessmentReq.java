package com.mycropdiary.api.dto.water;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Yêu cầu cập nhật phiếu đánh giá chất lượng nước (Update Water Assessment)")
public record UpdateWaterAssessmentReq(
        @Schema(description = "ID nguồn nước", example = "1")
        Integer waterSourceId,

        @Schema(description = "Tên nguồn nước")
        String waterSourceName,

        @Schema(description = "Loại nguồn nước")
        String waterSourceType,

        @Schema(description = "Độ pH của mẫu nước", example = "7.0")
        @DecimalMin(value = "0.0", message = "Độ pH không thể nhỏ hơn 0.0")
        @DecimalMax(value = "14.0", message = "Độ pH không thể lớn hơn 14.0")
        BigDecimal waterPh,

        @Schema(description = "Nhiệt độ mẫu nước (°C)", example = "22.0")
        BigDecimal temperatureC,

        @Schema(description = "Trạng thái / Đánh giá chất lượng", example = "DAT_CHUAN")
        String qualityStatus,

        @Schema(description = "Ghi chú, chỉ số xét nghiệm")
        String notes,

        @Schema(description = "URL ảnh chụp phiếu kết quả xét nghiệm mẫu nước")
        String imageUrl,

        @Schema(description = "Thời gian lấy mẫu và kiểm nghiệm")
        Instant assessedAt
) {}

