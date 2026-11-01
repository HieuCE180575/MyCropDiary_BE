package com.mycropdiary.api.dto.vietgap;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

@Schema(description = "Yêu cầu thực hiện đánh giá nội bộ VietGAP (Conduct Internal VietGAP Assessment)")
public record ConductInternalAssessmentReq(
        @Schema(description = "Tiêu đề đợt đánh giá nội bộ", example = "Đánh giá nội bộ VietGAP Quý 1/2026 - Nhà kính công nghệ cao")
        @NotBlank(message = "Tiêu đề đánh giá không được để trống")
        @Size(max = 180, message = "Tiêu đề tối đa 180 ký tự")
        String assessmentTitle,

        @Schema(description = "ID mùa vụ đánh giá (tùy chọn)", example = "1")
        Integer cropSeasonId,

        @Schema(description = "ID phân khu đánh giá (tùy chọn)", example = "1")
        Integer zoneId,

        @Schema(description = "Thời điểm đánh giá (mặc định hiện tại)")
        Instant assessedAt,

        @Schema(description = "Nhận xét / kết luận tổng quát của đợt đánh giá")
        String notes,

        @Schema(description = "Ảnh bằng chứng / biên bản tổng hợp đánh giá nội bộ")
        String evidenceImageUrl,

        @Schema(description = "Danh sách điểm số và bằng chứng cho từng tiêu chí đánh giá")
        @NotEmpty(message = "Danh sách tiêu chí đánh giá không được để trống")
        @Valid
        List<InternalAssessmentItemReq> items
) {
}

