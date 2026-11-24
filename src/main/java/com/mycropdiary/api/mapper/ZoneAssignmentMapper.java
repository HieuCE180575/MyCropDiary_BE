package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.assignment.AssignmentRespDTO;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffZoneAssignment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ZoneAssignmentMapper {

    private final ZoneMapper zoneMapper = new ZoneMapper();

    public AssignmentRespDTO toResponse(StaffZoneAssignment entity, FarmMember assigner) {
        return zoneMapper.toAssignmentResponse(entity, assigner);
    }

    public AssignmentRespDTO toResponse(StaffZoneAssignment entity) {
        return zoneMapper.toAssignmentResponse(entity, null);
    }

    public List<AssignmentRespDTO> toResponseList(List<StaffZoneAssignment> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).toList();
    }
}
