package com.mycropdiary.api.dto.admin;

public record UpdateChecklistRuleReqDTO(
        String ruleCode,
        String ruleName,
        String description,
        String category,
        String ruleLevel,
        String targetCropType,
        Boolean isActive
) {
    public UpdateChecklistRuleReqDTO(String ruleName, String description, String category, Boolean isActive) {
        this(null, ruleName, description, category, null, null, isActive);
    }
}
