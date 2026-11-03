package com.mycropdiary.api.dto.water;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Yêu cầu ghi nhận đánh giá chất lượng nước tưới tiêu (Create Water Assessment)")
public record CreateWaterAssessmentReq(
        @Schema(description = "ID nguồn nước (nếu có, hoặc để trống hệ thống tự liên kết/tạo)", example = "1")
        Integer waterSourceId,

        @Schema(description = "Tên nguồn nước", example = "Hồ chứa lắng lọc & Giếng khoan tầng sâu")
        String waterSourceName,

        @Schema(description = "Loại nguồn nước (Giếng ngầm, Hồ chứa, Sông suối...)", example = "Nước mặt & Hồ chứa")
        String waterSourceType,

        @Schema(description = "Độ pH của mẫu nước (0.0 - 14.0)", example = "6.8")
        @DecimalMin(value = "0.0", message = "Độ pH không thể nhỏ hơn 0.0")
        @DecimalMax(value = "14.0", message = "Độ pH không thể lớn hơn 14.0")
        BigDecimal waterPh,

        @Schema(description = "Nhiệt độ mẫu nước (°C)", example = "21.5")
        BigDecimal temperatureC,

        @Schema(description = "Trạng thái / Đánh giá chất lượng (DAT_CHUAN, CAN_XU_LY, KHONG_DAT)", example = "DAT_CHUAN")
        String qualityStatus,

        @Schema(description = "Ghi chú, chỉ số Coliform, kim loại nặng, biện pháp xử lý", example = "Chỉ số kim loại nặng và Coliform đạt chuẩn QCVN 39:2011/BTNMT phục vụ tưới tiêu VietGAP.")
        String notes,

        @Schema(description = "URL ảnh chụp phiếu kết quả xét nghiệm mẫu nước")
        String imageUrl,

        @Schema(description = "Thời gian lấy mẫu và kiểm nghiệm")
        Instant assessedAt
) {}

