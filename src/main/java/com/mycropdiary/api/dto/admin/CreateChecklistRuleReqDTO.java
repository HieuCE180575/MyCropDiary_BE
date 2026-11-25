package com.mycropdiary.api.dto.admin;

public record CreateChecklistRuleReqDTO(
        String ruleCode,
        String ruleName,
        String description,
        String category,
        String ruleLevel,
        String targetCropType,
        Boolean isActive
) {
    public CreateChecklistRuleReqDTO(String ruleName, String description, String category, Boolean isActive) {
        this(null, ruleName, description, category, "MUST", "ALL", isActive);
    }
}
