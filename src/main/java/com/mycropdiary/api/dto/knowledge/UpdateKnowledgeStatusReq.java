package com.mycropdiary.api.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateKnowledgeStatusReq(
        @NotBlank(message = "Trạng thái không được để trống")
        @Pattern(regexp = "(?i)^(draft|published|archived)$", message = "Trạng thái phải là draft, published hoặc archived")
        String status
) {
}

