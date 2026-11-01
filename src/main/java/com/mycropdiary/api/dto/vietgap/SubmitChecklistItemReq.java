package com.mycropdiary.api.dto.vietgap;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Yêu cầu ghi nhận kết quả và bằng chứng mục kiểm tra")
public record SubmitChecklistItemReq(
        @Schema(description = "Trạng thái kiểm tra ('pass', 'fail', 'not_applicable')", example = "pass")
        @NotBlank(message = "resultStatus không được để trống")
        String resultStatus,

        @Schema(description = "Ghi chú hiện trường", example = "Đã kiểm tra nhật ký cách ly thuốc BVTV, đủ 14 ngày an toàn")
        String notes,

        @Schema(description = "Đường dẫn ảnh bằng chứng kiểm tra hiện trường")
        String evidenceImageUrl
) {
}

