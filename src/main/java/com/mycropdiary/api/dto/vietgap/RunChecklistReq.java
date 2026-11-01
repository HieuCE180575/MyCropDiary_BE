package com.mycropdiary.api.dto.vietgap;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

@Schema(description = "Yêu cầu thực thi kiểm tra danh mục bằng chứng VietGAP (Run VietGAP Evidence Checklist)")
public record RunChecklistReq(
        @Schema(description = "ID quy tắc kiểm tra (ChecklistRule ID)", example = "1")
        @NotNull(message = "checklistRuleId không được để trống")
        Integer checklistRuleId,

        @Schema(description = "ID mùa vụ áp dụng kiểm tra", example = "1")
        Integer cropSeasonId,

        @Schema(description = "ID lô đất kiểm tra (tùy chọn)", example = "1")
        Integer blockId,

        @Schema(description = "ID thành viên phụ trách kiểm tra", example = "2")
        Integer assignedToMemberId,

        @Schema(description = "Thời gian lên lịch kiểm tra")
        Instant scheduledAt,

        @Schema(description = "Ghi chú nhanh khi kích hoạt run")
        String notes,

        @Schema(description = "Danh sách kết quả kiểm tra và bằng chứng (nếu nhập trực tiếp)")
        List<SubmitChecklistItemReq> items
) {
}

