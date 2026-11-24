package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.admin.ChecklistRuleDetailRespDTO;
import com.mycropdiary.api.dto.admin.ChecklistRuleRespDTO;
import com.mycropdiary.api.entity.cultivation.ChecklistRule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChecklistRuleMapper {

    public ChecklistRuleRespDTO toResponse(ChecklistRule r) {
        if (r == null) {
            return null;
        }
        return new ChecklistRuleRespDTO(
                r.getId(),
                r.getRuleName(),
                r.getDescription(),
                r.getCategory(),
                r.getIsActive(),
                r.getCreatedAt()
        );
    }

    public ChecklistRuleDetailRespDTO toDetailResponse(ChecklistRule r) {
        if (r == null) {
            return null;
        }
        return new ChecklistRuleDetailRespDTO(
                r.getId(),
                r.getRuleName(),
                r.getDescription(),
                r.getCategory(),
                r.getIsActive(),
                r.getCreatedAt()
        );
    }

    public List<ChecklistRuleRespDTO> toResponseList(List<ChecklistRule> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).toList();
    }
}
