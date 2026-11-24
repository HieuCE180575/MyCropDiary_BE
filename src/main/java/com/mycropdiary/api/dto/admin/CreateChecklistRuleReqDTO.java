package com.mycropdiary.api.dto.admin;

public record CreateChecklistRuleReqDTO(
        String ruleName,
        String description,
        String category,
        Boolean isActive
) {}
