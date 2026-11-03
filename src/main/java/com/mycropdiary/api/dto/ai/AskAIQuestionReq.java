package com.mycropdiary.api.dto.ai;

import jakarta.validation.constraints.NotBlank;

public record AskAIQuestionReq(
        @NotBlank(message = "Nội dung câu hỏi không được để trống")
        String question,
        Integer conversationId,
        Integer farmId
) {
}

