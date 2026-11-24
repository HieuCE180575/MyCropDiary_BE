package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.cultivation.ActivityWorkerRespDTO;
import com.mycropdiary.api.dto.cultivation.FarmingActivityRespDTO;
import com.mycropdiary.api.dto.cultivation.FarmingActivitySummaryRespDTO;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.FarmingActivity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lớp chuyển đổi dữ liệu (Mapper) giữa Entity FarmingActivity và DTOs.
 */
@Component
public class FarmingActivityMapper {

    public FarmingActivityRespDTO toResponse(FarmingActivity act, List<ActivityWorkerRespDTO> workers, FarmMember supervisor) {
        CropSeason cs = act.getCropSeason();
        Farm farm = cs != null ? cs.getFarm() : act.getFarm();
        Block block = act.getBlock();

        return new FarmingActivityRespDTO(
                act.getId(),
                cs != null ? cs.getId() : null,
                cs != null ? cs.getSeasonName() : null,
                cs != null ? cs.getSeasonName() : null,
                farm != null ? farm.getId() : null,
                farm != null ? farm.getFarmName() : null,
                block != null ? block.getId() : null,
                block != null ? block.getBlockName() : null,
                block != null ? block.getBlockName() : null,
                supervisor != null ? supervisor.getId() : null,
                supervisor != null && supervisor.getUser() != null ? supervisor.getUser().getFullName() : null,
                act.getActivityType(),
                act.getActivityName(),
                act.getStartedAt(),
                act.getEndedAt(),
                act.getDescription(),
                act.getResultNotes(),
                act.getWeatherNotes(),
                act.getFarmTaskId(),
                workers,
                act.getStartedAt()
        );
    }

    public FarmingActivitySummaryRespDTO toSummaryResponse(FarmingActivity act, FarmMember supervisor, int workerCount) {
        Block block = act.getBlock();

        return new FarmingActivitySummaryRespDTO(
                act.getId(),
                act.getCropSeason() != null ? act.getCropSeason().getId() : null,
                block != null ? block.getId() : null,
                block != null ? block.getBlockName() : null,
                act.getActivityType(),
                act.getActivityName(),
                act.getDescription(),
                act.getStartedAt(),
                act.getEndedAt(),
                supervisor != null ? supervisor.getId() : null,
                supervisor != null && supervisor.getUser() != null ? supervisor.getUser().getFullName() : null,
                workerCount,
                act.getStartedAt()
        );
    }
}
