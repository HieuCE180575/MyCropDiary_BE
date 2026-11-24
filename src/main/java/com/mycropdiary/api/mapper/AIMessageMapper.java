package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.ai.AIMessageRespDTO;
import com.mycropdiary.api.entity.AIMessage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AIMessageMapper {

    public AIMessageRespDTO toResponse(AIMessage entity, List<String> sources) {
        return AIMessageRespDTO.fromEntity(entity, sources);
    }
}
