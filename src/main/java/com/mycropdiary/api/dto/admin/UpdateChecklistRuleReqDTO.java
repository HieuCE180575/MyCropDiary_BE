package com.mycropdiary.api.dto.admin;

public record UpdateChecklistRuleReqDTO(
        String ruleName,
        String description,
        String category,
        Boolean isActive
) {}
