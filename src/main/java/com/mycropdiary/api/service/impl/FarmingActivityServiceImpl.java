package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResponse;
import com.mycropdiary.api.dto.cultivation.*;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.cultivation.*;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.StaffAreaAssignmentRepository;
import com.mycropdiary.api.repository.cultivation.*;
import com.mycropdiary.api.service.cultivation.FarmingActivityService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FarmingActivityServiceImpl implements FarmingActivityService {

    private final FarmingActivityRepository farmingActivityRepository;
    private final ActivityWorkerRepository activityWorkerRepository;
    private final CropSeasonRepository cropSeasonRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final FarmWorkerRepository farmWorkerRepository;
    private final StaffAreaAssignmentRepository staffAreaAssignmentRepository;

    public FarmingActivityServiceImpl(FarmingActivityRepository farmingActivityRepository,
                                      ActivityWorkerRepository activityWorkerRepository,
                                      CropSeasonRepository cropSeasonRepository,
                                      FarmMemberRepository farmMemberRepository,
                                      FarmWorkerRepository farmWorkerRepository,
                                      StaffAreaAssignmentRepository staffAreaAssignmentRepository) {
        this.farmingActivityRepository = farmingActivityRepository;
        this.activityWorkerRepository = activityWorkerRepository;
        this.cropSeasonRepository = cropSeasonRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.farmWorkerRepository = farmWorkerRepository;
        this.staffAreaAssignmentRepository = staffAreaAssignmentRepository;
    }

    private FarmMember validateAndGetActiveMember(Long userId, CropSeason cropSeason) {
        Farm farm = cropSeason.getFarm();
        FarmMember member = farmMemberRepository.findByFarmIdAndUserIdAndStatus(farm.getId(), userId, "ACTIVE")
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập hoặc ghi nhật ký cho mùa vụ của trang trại này."));

        if ("STAFF".equalsIgnoreCase(member.getFarmRole())) {
            Plot plot = cropSeason.getPlot();
            if (plot != null && plot.getProductionArea() != null) {
                Long productionAreaId = plot.getProductionArea().getId();
                boolean isAssigned = staffAreaAssignmentRepository.existsByFarmMemberIdAndProductionAreaIdAndActiveTrue(member.getId(), productionAreaId);
                if (!isAssigned) {
                    throw new ForbiddenException("Nhân viên chưa được phân công quản lý vùng sản xuất chứa mùa vụ này.");
                }
            }
        }

        return member;
    }

    @Override
    @Transactional
    public FarmingActivityResponse createActivity(Long currentUserId, CreateFarmingActivityRequest request) {
        CropSeason cropSeason = cropSeasonRepository.findById(request.cropSeasonId())
                .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại."));

        if ("COMPLETED".equalsIgnoreCase(cropSeason.getStatus()) || "CANCELLED".equalsIgnoreCase(cropSeason.getStatus())) {
            throw new BadRequestException("Không thể tạo nhật ký canh tác cho mùa vụ đã hoàn thành hoặc đã bị hủy.");
        }

        if (request.endedAt() != null && request.endedAt().isBefore(request.startedAt())) {
            throw new BadRequestException("Thời gian kết thúc không thể trước thời gian bắt đầu.");
        }

        // Tự động xác định SupervisedByMemberID từ tài khoản người dùng đã xác thực
        FarmMember supervisor = validateAndGetActiveMember(currentUserId, cropSeason);

        FarmingActivity activity = new FarmingActivity();
        activity.setCropSeason(cropSeason);
        activity.setSupervisedByMember(supervisor);
        activity.setActivityType(request.activityType().trim());
        activity.setActivityName(request.activityName().trim());
        activity.setStartedAt(request.startedAt());
        activity.setEndedAt(request.endedAt());
        activity.setDescription(request.description() != null ? request.description().trim() : null);
        activity.setResultNotes(request.resultNotes() != null ? request.resultNotes().trim() : null);
        activity.setWeatherNotes(request.weatherNotes() != null ? request.weatherNotes().trim() : null);
        activity.setFarmTaskId(request.farmTaskId());

        FarmingActivity savedActivity = farmingActivityRepository.save(activity);

        List<ActivityWorkerResponse> workerResponses = new ArrayList<>();

        if (request.workers() != null && !request.workers().isEmpty()) {
            List<Long> workerIds = request.workers().stream()
                    .map(ActivityWorkerRequest::workerId)
                    .distinct()
                    .toList();

            List<FarmWorker> farmWorkers = farmWorkerRepository.findByIdInAndFarmId(workerIds, cropSeason.getFarm().getId());
            if (farmWorkers.size() != workerIds.size()) {
                throw new BadRequestException("Một hoặc nhiều công nhân không tồn tại hoặc không thuộc trang trại của mùa vụ này.");
            }

            Map<Long, FarmWorker> workerMap = farmWorkers.stream()
                    .collect(Collectors.toMap(FarmWorker::getId, w -> w));

            List<ActivityWorker> activityWorkers = new ArrayList<>();
            for (ActivityWorkerRequest workerReq : request.workers()) {
                FarmWorker fw = workerMap.get(workerReq.workerId());
                ActivityWorker aw = new ActivityWorker(savedActivity, fw, workerReq.workHours(), workerReq.notes());
                activityWorkers.add(aw);
                workerResponses.add(new ActivityWorkerResponse(
                        fw.getId(),
                        fw.getWorkerCode(),
                        fw.getFullName(),
                        workerReq.workHours(),
                        workerReq.notes()
                ));
            }
            activityWorkerRepository.saveAll(activityWorkers);
        }

        return mapToResponse(savedActivity, workerResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FarmingActivitySummaryResponse> getActivitiesByCropSeason(Long currentUserId, Long cropSeasonId, Pageable pageable) {
        CropSeason cropSeason = cropSeasonRepository.findById(cropSeasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại."));

        validateAndGetActiveMember(currentUserId, cropSeason);

        Page<FarmingActivity> page = farmingActivityRepository.findByCropSeasonId(cropSeasonId, pageable);
        return PageResponse.map(page, this::mapToSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FarmingActivityResponse getActivityDetail(Long currentUserId, Long activityId) {
        FarmingActivity activity = farmingActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhật ký canh tác không tồn tại."));

        validateAndGetActiveMember(currentUserId, activity.getCropSeason());

        List<ActivityWorker> workers = activityWorkerRepository.findByFarmingActivityId(activityId);
        List<ActivityWorkerResponse> workerResponses = workers.stream()
                .map(aw -> new ActivityWorkerResponse(
                        aw.getFarmWorker().getId(),
                        aw.getFarmWorker().getWorkerCode(),
                        aw.getFarmWorker().getFullName(),
                        aw.getWorkHours(),
                        aw.getNotes()
                ))
                .toList();

        return mapToResponse(activity, workerResponses);
    }

    private FarmingActivityResponse mapToResponse(FarmingActivity act, List<ActivityWorkerResponse> workers) {
        CropSeason cs = act.getCropSeason();
        Plot plot = cs.getPlot();
        Farm farm = cs.getFarm();
        FarmMember supervisor = act.getSupervisedByMember();

        return new FarmingActivityResponse(
                act.getId(),
                cs.getId(),
                cs.getSeasonCode(),
                cs.getSeasonName(),
                farm.getId(),
                farm.getFarmName(),
                plot.getId(),
                plot.getPlotCode(),
                plot.getPlotName(),
                supervisor.getId(),
                supervisor.getUser() != null ? supervisor.getUser().getFullName() : null,
                act.getActivityType(),
                act.getActivityName(),
                act.getStartedAt(),
                act.getEndedAt(),
                act.getDescription(),
                act.getResultNotes(),
                act.getWeatherNotes(),
                act.getFarmTaskId(),
                workers,
                act.getCreatedAt()
        );
    }

    private FarmingActivitySummaryResponse mapToSummaryResponse(FarmingActivity act) {
        List<ActivityWorker> workers = activityWorkerRepository.findByFarmingActivityId(act.getId());
        FarmMember supervisor = act.getSupervisedByMember();

        return new FarmingActivitySummaryResponse(
                act.getId(),
                act.getCropSeason().getId(),
                act.getActivityType(),
                act.getActivityName(),
                act.getStartedAt(),
                act.getEndedAt(),
                supervisor.getId(),
                supervisor.getUser() != null ? supervisor.getUser().getFullName() : null,
                workers.size(),
                act.getCreatedAt()
        );
    }
}
