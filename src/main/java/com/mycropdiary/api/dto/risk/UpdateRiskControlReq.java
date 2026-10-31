package com.mycropdiary.api.dto.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Yêu cầu cập nhật biện pháp kiểm soát rủi ro (Update Risk Control Measures)")
public record UpdateRiskControlReq(
        @Schema(description = "Biện pháp kiểm soát & phòng ngừa mới áp dụng", example = "Đã hoàn thành trồng hàng rào keo lá tràm chắn gió và lắp đặt vòi phun sương vùng đệm")
        @NotBlank(message = "Biện pháp kiểm soát không được để trống")
        String mitigationPlan,

        @Schema(description = "Mức độ rủi ro cập nhật sau kiểm soát (low, medium, high, critical)", example = "low")
        String riskLevel,

        @Schema(description = "Ghi chú bổ sung")
        String description,

        @Schema(description = "Ảnh kiểm tra hiện trường sau can thiệp")
        String imageUrl
) {
}

