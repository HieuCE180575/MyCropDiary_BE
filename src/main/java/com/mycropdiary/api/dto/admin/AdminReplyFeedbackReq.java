package com.mycropdiary.api.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminReplyFeedbackReq(
        @NotBlank(message = "Nội dung phản hồi không được để trống")
        String replyText
) {}

