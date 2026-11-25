package com.mycropdiary.api.dto.compliance;

import jakarta.validation.constraints.NotBlank;

public record ResolveComplaintReqDTO(
        @NotBlank(message = "Trạng thái xử lý không được để trống")
        String status, // 'investigating', 'resolved'

        @NotBlank(message = "Nội dung phương án xử lý / khắc phục không được để trống")
        String resolutionNotes
) {}
