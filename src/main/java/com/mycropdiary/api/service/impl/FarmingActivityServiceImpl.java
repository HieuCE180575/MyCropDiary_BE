package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.*;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.entity.cultivation.*;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.FarmingActivityMapper;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.StaffZoneAssignmentRepository;
import com.mycropdiary.api.repository.cultivation.*;
import com.mycropdiary.api.service.cultivation.FarmingActivityService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class FarmingActivityServiceImpl implements FarmingActivityService {

    private final FarmingActivityRepository farmingActivityRepository;
    private final ActivityWorkerRepository activityWorkerRepository;
    private final CropSeasonRepository cropSeasonRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final FarmWorkerRepository farmWorkerRepository;
    private final StaffZoneAssignmentRepository staffZoneAssignmentRepository;
    private FarmingActivityMapper farmingActivityMapper = new FarmingActivityMapper();

    public FarmingActivityServiceImpl(FarmingActivityRepository farmingActivityRepository,
                                      ActivityWorkerRepository activityWorkerRepository,
                                      CropSeasonRepository cropSeasonRepository,
                                      FarmMemberRepository farmMemberRepository,
                                      FarmWorkerRepository farmWorkerRepository,
                                      StaffZoneAssignmentRepository staffZoneAssignmentRepository) {
        this.farmingActivityRepository = farmingActivityRepository;
        this.activityWorkerRepository = activityWorkerRepository;
        this.cropSeasonRepository = cropSeasonRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.farmWorkerRepository = farmWorkerRepository;
        this.staffZoneAssignmentRepository = staffZoneAssignmentRepository;
    }

    @Autowired(required = false)
    public void setFarmingActivityMapper(FarmingActivityMapper farmingActivityMapper) {
        if (farmingActivityMapper != null) {
            this.farmingActivityMapper = farmingActivityMapper;
        }
    }

    private FarmMember validateAndGetActiveMember(Integer userId, CropSeason cropSeason) {
        Farm farm = cropSeason.getFarm();
        return farmMemberRepository.findByFarmIdAndUserIdAndStatus(farm.getId(), userId, "active")
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền truy cập hoặc ghi nhật ký cho mùa vụ của trang trại này."));
    }

    @Override
    @Transactional
    public FarmingActivityResp createActivity(Integer currentUserId, CreateFarmingActivityReq request) {
        CropSeason cropSeason = cropSeasonRepository.findById(request.cropSeasonId())
                .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại."));

        if ("completed".equalsIgnoreCase(cropSeason.getSeasonStatus()) || "cancelled".equalsIgnoreCase(cropSeason.getSeasonStatus())) {
            throw new BadRequestException("Không thể tạo nhật ký canh tác cho mùa vụ đã hoàn thành hoặc đã bị hủy.");
        }

        if (request.endedAt() != null && request.endedAt().isBefore(request.startedAt())) {
            throw new BadRequestException("Thời gian kết thúc không thể trước thời gian bắt đầu.");
        }

        FarmMember supervisor = validateAndGetActiveMember(currentUserId, cropSeason);

        FarmingActivity activity = new FarmingActivity();
        activity.setFarm(cropSeason.getFarm());
        activity.setCropSeason(cropSeason);
        activity.setCreatedBy(supervisor.getUser());
        activity.setActivityType(request.activityType().trim());
        activity.setActivityName(request.activityName().trim());
        activity.setStartedAt(request.startedAt());
        activity.setEndedAt(request.endedAt());
        activity.setDescription(request.description() != null ? request.description().trim() : null);
        activity.setResultNotes(request.resultNotes() != null ? request.resultNotes().trim() : null);
        activity.setWeatherNotes(request.weatherNotes() != null ? request.weatherNotes().trim() : null);
        activity.setFarmTaskId(request.farmTaskId());

        FarmingActivity savedActivity = farmingActivityRepository.save(activity);

        List<ActivityWorkerResp> workerResponses = new ArrayList<>();

        if (request.workers() != null && !request.workers().isEmpty()) {
            List<Integer> workerIds = request.workers().stream()
                    .map(ActivityWorkerReq::workerId)
                    .distinct()
                    .toList();

            List<FarmWorker> farmWorkers = farmWorkerRepository.findByIdInAndFarmId(workerIds, cropSeason.getFarm().getId());
            if (farmWorkers.size() != workerIds.size()) {
                throw new BadRequestException("Một hoặc nhiều công nhân không tồn tại hoặc không thuộc trang trại của mùa vụ này.");
            }

            Map<Integer, FarmWorker> workerMap = farmWorkers.stream()
                    .collect(Collectors.toMap(FarmWorker::getId, w -> w));

            List<ActivityWorker> activityWorkers = new ArrayList<>();
            for (ActivityWorkerReq workerReq : request.workers()) {
                FarmWorker fw = workerMap.get(workerReq.workerId());
                ActivityWorker aw = new ActivityWorker(savedActivity, fw, workerReq.workHours(), workerReq.notes());
                activityWorkers.add(aw);
                workerResponses.add(new ActivityWorkerResp(
                        fw.getId(),
                        fw.getWorkerCode(),
                        fw.getFullName(),
                        workerReq.workHours(),
                        workerReq.notes()
                ));
            }
            activityWorkerRepository.saveAll(activityWorkers);
        }

        return farmingActivityMapper.toResponse(savedActivity, workerResponses, supervisor);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResp<FarmingActivitySummaryResp> getActivitiesByCropSeason(Integer currentUserId, Integer cropSeasonId, Pageable pageable) {
        return searchAndFilterActivities(currentUserId, cropSeasonId, null, null, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResp<FarmingActivitySummaryResp> searchAndFilterActivities(
            Integer currentUserId,
            Integer cropSeasonId,
            String keyword,
            String activityType,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Pageable pageable) {
        CropSeason cropSeason = cropSeasonRepository.findById(cropSeasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ canh tác không tồn tại."));

        FarmMember member = validateAndGetActiveMember(currentUserId, cropSeason);

        Specification<FarmingActivity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("cropSeason").get("id"), cropSeasonId));

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("activityName")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("activityNotes")), pattern);
                predicates.add(cb.or(nameMatch, descMatch));
            }

            if (activityType != null && !activityType.trim().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("activityType")), activityType.trim().toLowerCase()));
            }

            if (fromDate != null) {
                Instant fromInstant = fromDate.atZone(ZoneId.systemDefault()).toInstant();
                predicates.add(cb.greaterThanOrEqualTo(root.get("performedAt"), fromInstant));
            }

            if (toDate != null) {
                Instant toInstant = toDate.atZone(ZoneId.systemDefault()).toInstant();
                predicates.add(cb.lessThanOrEqualTo(root.get("performedAt"), toInstant));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<FarmingActivity> page = farmingActivityRepository.findAll(spec, pageable);
        return PageResp.map(page, act -> {
            List<ActivityWorker> workers = activityWorkerRepository.findByFarmingActivityId(act.getId());
            return farmingActivityMapper.toSummary(act, member, workers);
        });
    }

    @Override
    @Transactional
    public FarmingActivityResp updateActivity(Integer currentUserId, Integer activityId, UpdateFarmingActivityReq request) {
        FarmingActivity activity = farmingActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhật ký canh tác không tồn tại."));

        CropSeason cropSeason = activity.getCropSeason();
        if ("completed".equalsIgnoreCase(cropSeason.getSeasonStatus()) || "cancelled".equalsIgnoreCase(cropSeason.getSeasonStatus())) {
            throw new BadRequestException("Không thể chỉnh sửa nhật ký canh tác của mùa vụ đã hoàn thành hoặc đã bị hủy.");
        }

        if (request.endedAt() != null && request.endedAt().isBefore(request.startedAt())) {
            throw new BadRequestException("Thời gian kết thúc không thể trước thời gian bắt đầu.");
        }

        FarmMember supervisor = validateAndGetActiveMember(currentUserId, cropSeason);

        activity.setActivityType(request.activityType().trim());
        activity.setActivityName(request.activityName().trim());
        activity.setStartedAt(request.startedAt());
        activity.setEndedAt(request.endedAt());
        activity.setDescription(request.description() != null ? request.description().trim() : null);
        activity.setResultNotes(request.resultNotes() != null ? request.resultNotes().trim() : null);
        activity.setWeatherNotes(request.weatherNotes() != null ? request.weatherNotes().trim() : null);
        activity.setFarmTaskId(request.farmTaskId());

        FarmingActivity savedActivity = farmingActivityRepository.save(activity);

        List<ActivityWorkerResp> workerResponses = new ArrayList<>();

        if (request.workers() != null) {
            activityWorkerRepository.deleteByFarmingActivityId(savedActivity.getId());
            activityWorkerRepository.flush();

            if (!request.workers().isEmpty()) {
                List<Integer> workerIds = request.workers().stream()
                        .map(ActivityWorkerReq::workerId)
                        .distinct()
                        .toList();

                List<FarmWorker> farmWorkers = farmWorkerRepository.findByIdInAndFarmId(workerIds, cropSeason.getFarm().getId());
                if (farmWorkers.size() != workerIds.size()) {
                    throw new BadRequestException("Một hoặc nhiều công nhân không tồn tại hoặc không thuộc trang trại của mùa vụ này.");
                }

                Map<Integer, FarmWorker> workerMap = farmWorkers.stream()
                        .collect(Collectors.toMap(FarmWorker::getId, w -> w));

                List<ActivityWorker> activityWorkers = new ArrayList<>();
                for (ActivityWorkerReq workerReq : request.workers()) {
                    FarmWorker fw = workerMap.get(workerReq.workerId());
                    ActivityWorker aw = new ActivityWorker(savedActivity, fw, workerReq.workHours(), workerReq.notes());
                    activityWorkers.add(aw);
                    workerResponses.add(new ActivityWorkerResp(
                            fw.getId(),
                            fw.getWorkerCode(),
                            fw.getFullName(),
                            workerReq.workHours(),
                            workerReq.notes()
                    ));
                }
                activityWorkerRepository.saveAll(activityWorkers);
            }
        } else {
            List<ActivityWorker> existingWorkers = activityWorkerRepository.findByFarmingActivityId(savedActivity.getId());
            for (ActivityWorker aw : existingWorkers) {
                workerResponses.add(new ActivityWorkerResp(
                        aw.getFarmWorker().getId(),
                        aw.getFarmWorker().getWorkerCode(),
                        aw.getFarmWorker().getFullName(),
                        aw.getWorkHours(),
                        aw.getNotes()
                ));
            }
        }

        return farmingActivityMapper.toResponse(savedActivity, workerResponses, supervisor);
    }


    @Override
    @Transactional(readOnly = true)
    public FarmingActivityResp getActivityDetail(Integer currentUserId, Integer activityId) {
        FarmingActivity activity = farmingActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhật ký canh tác không tồn tại."));

        FarmMember member = validateAndGetActiveMember(currentUserId, activity.getCropSeason());

        List<ActivityWorker> workers = activityWorkerRepository.findByFarmingActivityId(activityId);
        List<ActivityWorkerResp> workerResponses = workers.stream()
                .map(aw -> new ActivityWorkerResp(
                        aw.getFarmWorker().getId(),
                        aw.getFarmWorker().getWorkerCode(),
                        aw.getFarmWorker().getFullName(),
                        aw.getWorkHours(),
                        aw.getNotes()
                ))
                .toList();

        return farmingActivityMapper.toResponse(activity, workerResponses, member);
    }
}
