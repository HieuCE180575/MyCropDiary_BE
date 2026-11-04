package com.mycropdiary.api.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Thông tin phản hồi câu trả lời AI (Admin AI Feedback Details)")
public record AdminAIFeedbackResp(
        Integer feedbackId,
        Integer messageId,
        String messageText,
        String userQuestion,
        Integer userId,
        String senderName,
        String senderEmail,
        Integer rating,
        String feedbackText,
        String status,
        Instant createdAt,
        List<ReplyItem> replies
) {
    public record ReplyItem(
            Integer replyId,
            String replyText,
            String replierName,
            Instant createdAt
    ) {}
}

