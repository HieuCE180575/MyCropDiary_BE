package com.mycropdiary.api.dto.vietgap;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Yêu cầu cập nhật kết quả và bằng chứng tiêu chí kiểm tra VietGAP")
public record UpdateChecklistItemReqDTO(
        @Schema(description = "Trạng thái kiểm tra ('pass', 'fail', 'not_applicable', 'pending')", example = "pass")
        @NotBlank(message = "resultStatus không được để trống")
        String resultStatus,

        @Schema(description = "Ghi chú hiện trường thực tế hoặc hành động khắc phục", example = "Kho thông thoáng, có cửa khóa và đã dán biển cảnh báo nguy hiểm")
        String notes,

        @Schema(description = "Đường dẫn ảnh bằng chứng hiện trường")
        String evidenceImageUrl,

        @Schema(description = "Loại tham chiếu minh chứng nếu có", example = "MANUAL_INSPECTION")
        String evidenceReferenceType,

        @Schema(description = "ID bản ghi minh chứng liên kết nếu có", example = "10")
        Integer evidenceReferenceId
) {
}

