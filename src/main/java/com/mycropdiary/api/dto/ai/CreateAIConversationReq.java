package com.mycropdiary.api.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAIConversationReq(
        @NotBlank(message = "Tiêu đề hội thoại không được để trống")
        @Size(max = 200, message = "Tiêu đề hội thoại tối đa 200 ký tự")
        String title,
        Integer farmId
) {
}

