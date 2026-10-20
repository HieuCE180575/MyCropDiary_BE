package com.mycropdiary.api.dto.block;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * DTO yêu cầu ghi nhận tình trạng lô đất (UC-17.4: Record Block Condition).
 */
public record RecordBlockConditionReq(
        String conditionType,
        @NotBlank(message = "Mô tả tình trạng lô đất không được để trống")
        String description,
        String imageUrl,
        Instant observedAt
) {}
