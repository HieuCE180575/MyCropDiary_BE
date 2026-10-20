package com.mycropdiary.api.dto.block;

import java.time.Instant;

/**
 * DTO phản hồi bản ghi tình trạng lô đất (UC-17.4: Record Block Condition).
 */
public record BlockConditionResp(
        Integer observationId,
        Integer blockId,
        String blockName,
        Integer recordedByMemberId,
        String recorderFullName,
        String conditionType,
        String description,
        String imageUrl,
        Instant observedAt
) {}
