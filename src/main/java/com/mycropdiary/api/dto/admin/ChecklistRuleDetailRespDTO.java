package com.mycropdiary.api.dto.admin;

import java.time.Instant;

public record ChecklistRuleDetailRespDTO(
        Integer ruleId,
        String ruleName,
        String description,
        String category,
        Boolean isActive,
        Instant createdAt
) {}
