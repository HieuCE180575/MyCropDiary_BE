package com.mycropdiary.api.dto.ai;

import java.time.Instant;

public record AIConversationResp(
        Integer conversationId,
        String title,
        String status,
        Instant createdAt,
        Instant updatedAt,
        long messageCount,
        String lastMessageSnippet
) {
}

