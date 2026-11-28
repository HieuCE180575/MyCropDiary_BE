package com.mycropdiary.api.dto.admin;

import java.time.Instant;

public record ChecklistRuleRespDTO(
        Integer ruleId,
        String ruleCode,
        String ruleName,
        String description,
        String category,
        String ruleLevel,
        String targetCropType,
        Integer versionNumber,
        Boolean isActive,
        Instant createdAt
) {
    public ChecklistRuleRespDTO(Integer ruleId, String ruleCode, String ruleName, String description, String category,
                                String ruleLevel, String targetCropType, Boolean isActive, Instant createdAt) {
        this(ruleId, ruleCode, ruleName, description, category, ruleLevel, targetCropType, 1, isActive, createdAt);
    }

    public ChecklistRuleRespDTO(Integer ruleId, String ruleName, String description, String category, Boolean isActive, Instant createdAt) {
        this(ruleId, null, ruleName, description, category, "MUST", "ALL", 1, isActive, createdAt);
    }
}
