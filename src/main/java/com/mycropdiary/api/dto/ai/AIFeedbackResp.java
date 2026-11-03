package com.mycropdiary.api.dto.ai;

import com.mycropdiary.api.entity.AIFeedback;
import com.mycropdiary.api.entity.AIFeedbackReply;
import java.time.Instant;
import java.util.List;

public record AIFeedbackResp(
        Integer feedbackId,
        Integer messageId,
        String aiResponseSnippet,
        Integer rating,
        String feedbackText,
        Instant createdAt,
        List<String> replies
) {
    public static AIFeedbackResp fromEntity(AIFeedback entity) {
        if (entity == null) return null;
        String snippet = entity.getMessage() != null ? entity.getMessage().getMessageText() : "";
        if (snippet != null && snippet.length() > 120) {
            snippet = snippet.substring(0, 117) + "...";
        }
        List<String> replyTexts = entity.getReplies() != null
                ? entity.getReplies().stream().map(AIFeedbackReply::getReplyText).toList()
                : List.of();

        return new AIFeedbackResp(
                entity.getId(),
                entity.getMessage() != null ? entity.getMessage().getId() : null,
                snippet,
                entity.getRating(),
                entity.getFeedbackText(),
                entity.getCreatedAt(),
                replyTexts
        );
    }
}

