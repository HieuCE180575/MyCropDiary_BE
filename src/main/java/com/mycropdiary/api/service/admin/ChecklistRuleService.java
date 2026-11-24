package com.mycropdiary.api.service.admin;

import com.mycropdiary.api.dto.admin.ChecklistRuleRespDTO;
import com.mycropdiary.api.dto.admin.CreateChecklistRuleReqDTO;
import com.mycropdiary.api.dto.admin.UpdateChecklistRuleReqDTO;

import java.util.List;

public interface ChecklistRuleService {
    List<ChecklistRuleRespDTO> getChecklistRules();
    ChecklistRuleRespDTO createChecklistRule(CreateChecklistRuleReqDTO req);
    ChecklistRuleRespDTO updateChecklistRule(Integer id, UpdateChecklistRuleReqDTO req);
}
