package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.worker.FarmWorkerRespDTO;
import com.mycropdiary.api.entity.cultivation.FarmWorker;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FarmWorkerMapper {

    public FarmWorkerRespDTO toResponse(FarmWorker entity) {
        return FarmWorkerRespDTO.fromEntity(entity);
    }

    public List<FarmWorkerRespDTO> toResponseList(List<FarmWorker> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).toList();
    }
}
