package com.mycropdiary.api.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Kết quả bóc tách hoạt động canh tác từ AI để tự động điền form")
public record FarmingDraftRespDTO(
        @Schema(description = "Loại hoạt động (Fertilizing, Spraying, Watering, Harvesting, Planting, Pruning, SoilCare, Other)", example = "Fertilizing")
        String activityType,

        @Schema(description = "Tên tóm tắt hoạt động", example = "Bón phân hữu cơ vi sinh")
        String activityName,

        @Schema(description = "Tên lô đất phù hợp nhất trong danh sách lô", example = "Lô A1")
        String matchedBlockName,

        @Schema(description = "Mô tả chi tiết, liều lượng, số lượng", example = "Bón phân hữu cơ vi sinh 20kg. Thời gian cách ly: 7 ngày.")
        String description,

        @Schema(description = "Ghi chú điều kiện thời tiết", example = "Trời nắng ráo")
        String weatherNotes,

        @Schema(description = "Ghi chú kết quả hoặc khuyến cáo kỹ thuật", example = "Cách ly 7 ngày trước thu hoạch")
        String resultNotes
) {
}
