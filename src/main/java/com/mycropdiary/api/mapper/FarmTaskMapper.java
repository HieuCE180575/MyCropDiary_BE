package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.cultivation.FarmTaskRespDTO;
import com.mycropdiary.api.dto.cultivation.TaskWorkerRespDTO;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.FarmTask;
import com.mycropdiary.api.entity.cultivation.FarmWorker;
import com.mycropdiary.api.entity.cultivation.TaskAssignment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp chuyển đổi dữ liệu (Mapper) giữa Entity FarmTask và FarmTaskResp DTO.
 */
@Component
public class FarmTaskMapper {

    public FarmTaskRespDTO toResponse(FarmTask t) {
        CropSeason cs = t.getCropSeason();
        Zone z = t.getZone();
        Block b = t.getBlock();
        User u = t.getCreatedBy();

        List<TaskWorkerRespDTO> assignees = new ArrayList<>();
        if (t.getAssignments() != null) {
            for (TaskAssignment a : t.getAssignments()) {
                FarmWorker fw = a.getFarmWorker();
                if (fw != null) {
                    assignees.add(new TaskWorkerRespDTO(
                            fw.getId(),
                            fw.getWorkerCode(),
                            fw.getFullName(),
                            fw.getPhoneNumber(),
                            fw.getJobTitle(),
                            a.getAssignmentStatus()
                    ));
                }
            }
        }

        return new FarmTaskRespDTO(
                t.getId(),
                t.getFarm().getId(),
                t.getFarm().getFarmName(),
                cs != null ? cs.getId() : null,
                cs != null ? cs.getSeasonName() : null,
                z != null ? z.getId() : null,
                z != null ? z.getZoneName() : null,
                b != null ? b.getId() : null,
                b != null ? b.getBlockName() : null,
                t.getTaskName(),
                t.getDescription(),
                t.getTaskType(),
                t.getPriority(),
                t.getStatus(),
                t.getStartsAt(),
                t.getDueAt(),
                u != null ? u.getId() : null,
                u != null ? u.getFullName() : null,
                assignees,
                t.getCreatedAt()
        );
    }
}
