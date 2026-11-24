package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.vietgap.ChecklistResultItemRespDTO;
import com.mycropdiary.api.dto.vietgap.ChecklistResultRespDTO;
import com.mycropdiary.api.entity.cultivation.ChecklistRun;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChecklistReviewMapper {

    public ChecklistResultRespDTO toResponse(ChecklistRun entity, List<ChecklistResultItemRespDTO> items) {
        return ChecklistResultRespDTO.fromEntity(entity, items);
    }
}
