package com.mycropdiary.api.dto.vietgap;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

@Schema(description = "Yêu cầu khởi tạo đợt tự đánh giá VietGAP toàn diện (33 tiêu chí)")
public record InitVietGapAuditRunReqDTO(
        @Schema(description = "ID mùa vụ áp dụng kiểm tra", example = "1")
        @NotNull(message = "cropSeasonId không được để trống")
        Integer cropSeasonId,

        @Schema(description = "ID lô đất / nhà màng (tùy chọn)", example = "1")
        Integer blockId,

        @Schema(description = "ID thành viên phụ trách kiểm tra (tùy chọn)", example = "2")
        Integer assignedToMemberId,

        @Schema(description = "Thời gian lên lịch kiểm tra (tùy chọn)")
        Instant scheduledAt,

        @Schema(description = "Ghi chú ban đầu khi kích hoạt đợt kiểm tra")
        String notes
) {
}

