package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.cultivation.ActivityWorkerResp;
import com.mycropdiary.api.dto.cultivation.FarmingActivityResp;
import com.mycropdiary.api.dto.cultivation.FarmingActivitySummaryResp;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.cultivation.ActivityWorker;
import com.mycropdiary.api.entity.cultivation.CropSeason;
import com.mycropdiary.api.entity.cultivation.FarmingActivity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper chuyển đổi giữa Entity FarmingActivity và các DTOs.
 */
@Component
public class FarmingActivityMapper {

    public FarmingActivityResp toResponse(FarmingActivity act, List<ActivityWorkerResp> workers, FarmMember supervisor) {
        if (act == null) {
            return null;
        }
        CropSeason cs = act.getCropSeason();
        Farm farm = cs != null ? cs.getFarm() : act.getFarm();
        Block block = act.getBlock();

        return new FarmingActivityResp(
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

    public FarmingActivitySummaryResp toSummary(FarmingActivity act, FarmMember supervisor, List<ActivityWorker> workers) {
        if (act == null) {
            return null;
        }
        int workerCount = workers != null ? workers.size() : 0;

        return new FarmingActivitySummaryResp(
                act.getId(),
                act.getCropSeason() != null ? act.getCropSeason().getId() : null,
                act.getActivityType(),
                act.getActivityName(),
                act.getStartedAt(),
                act.getEndedAt(),
                supervisor != null ? supervisor.getId() : null,
                supervisor != null && supervisor.getUser() != null ? supervisor.getUser().getFullName() : null,
                workerCount,
                act.getStartedAt()
        );
    }
}
