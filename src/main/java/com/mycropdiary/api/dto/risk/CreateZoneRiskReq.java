package com.mycropdiary.api.dto.risk;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Yêu cầu ghi nhận rủi ro sản xuất phân khu (Record Zone Production Risk)")
public record CreateZoneRiskReq(
        @Schema(description = "Loại rủi ro phát hiện", example = "Trôi dạt hóa chất lân cận")
        @NotBlank(message = "Loại rủi ro không được để trống")
        @Size(max = 80, message = "Loại rủi ro tối đa 80 ký tự")
        String riskType,

        @Schema(description = "Mức độ rủi ro (low, medium, high, critical)", example = "medium")
        @NotBlank(message = "Mức độ rủi ro không được để trống")
        String riskLevel,

        @Schema(description = "Mô tả chi tiết rủi ro", example = "Phía Đông giáp ranh hộ canh tác truyền thống có phun thuốc BVTV")
        String description,

        @Schema(description = "Biện pháp kiểm soát & giảm thiểu rủi ro (Risk Control Measures)", example = "Trồng hàng rào cây chắn gió cao 2.5m và thiết lập vùng đệm cách ly 5m")
        String mitigationPlan,

        @Schema(description = "Ảnh minh chứng hiện trường")
        String imageUrl,

        @Schema(description = "Thời điểm đánh giá (mặc định hiện tại)")
        Instant assessedAt
) {
}

