package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.*;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.cultivation.*;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ForbiddenException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.BlockRepository;
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
    private final BlockRepository blockRepository;
    private final CropSeasonBlockRepository cropSeasonBlockRepository;

    @Autowired
    public FarmingActivityServiceImpl(FarmingActivityRepository farmingActivityRepository,
                                      ActivityWorkerRepository activityWorkerRepository,
                                      CropSeasonRepository cropSeasonRepository,
                                      FarmMemberRepository farmMemberRepository,
                                      FarmWorkerRepository farmWorkerRepository,
                                      StaffZoneAssignmentRepository staffZoneAssignmentRepository,
                                      BlockRepository blockRepository,
                                      CropSeasonBlockRepository cropSeasonBlockRepository) {
        this.farmingActivityRepository = farmingActivityRepository;
        this.activityWorkerRepository = activityWorkerRepository;
        this.cropSeasonRepository = cropSeasonRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.farmWorkerRepository = farmWorkerRepository;
        this.staffZoneAssignmentRepository = staffZoneAssignmentRepository;
        this.blockRepository = blockRepository;
        this.cropSeasonBlockRepository = cropSeasonBlockRepository;
    }

    public FarmingActivityServiceImpl(FarmingActivityRepository farmingActivityRepository,
                                      ActivityWorkerRepository activityWorkerRepository,
                                      CropSeasonRepository cropSeasonRepository,
                                      FarmMemberRepository farmMemberRepository,
                                      FarmWorkerRepository farmWorkerRepository,
                                      StaffZoneAssignmentRepository staffZoneAssignmentRepository) {
        this(farmingActivityRepository, activityWorkerRepository, cropSeasonRepository,
                farmMemberRepository, farmWorkerRepository, staffZoneAssignmentRepository, null, null);
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

        String desc = request.description() != null ? request.description().trim() : "";
        String resNotes = request.resultNotes() != null ? request.resultNotes().trim() : "";
        if (!desc.isEmpty() && !resNotes.isEmpty() && !desc.equals(resNotes)) {
            activity.setDescription(desc + " | Kết quả: " + resNotes);
        } else if (!desc.isEmpty()) {
            activity.setDescription(desc);
        } else if (!resNotes.isEmpty()) {
            activity.setResultNotes(resNotes);
        }

        activity.setWeatherNotes(request.weatherNotes() != null ? request.weatherNotes().trim() : null);
        activity.setFarmTaskId(request.farmTaskId());

        if (request.blockId() != null && blockRepository != null) {
            if (cropSeasonBlockRepository != null) {
                List<CropSeasonBlock> assignedBlocks = cropSeasonBlockRepository.findByCropSeasonIdWithDetails(cropSeason.getId());
                if (!assignedBlocks.isEmpty() && !cropSeasonBlockRepository.existsByIdCropSeasonIdAndIdBlockId(cropSeason.getId(), request.blockId())) {
                    throw new BadRequestException("Lô đất được chọn chưa được phân bổ vào mùa vụ canh tác này.");
                }
            }
            Block block = blockRepository.findById(request.blockId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lô đất canh tác không tồn tại."));
            activity.setBlock(block);
            if (block.getZone() != null) {
                activity.setZone(block.getZone());
            }
        }

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

        return mapToResponse(savedActivity, workerResponses, supervisor);
    }

    @Override
    @Transactional
    public PageResp<FarmingActivitySummaryResp> getActivitiesByCropSeason(Integer currentUserId, Integer cropSeasonId, Pageable pageable) {
        return searchAndFilterActivities(currentUserId, cropSeasonId, null, null, null, null, pageable);
    }

    @Override
    @Transactional
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
        boolean noFilter = (keyword == null || keyword.trim().isEmpty())
                && (activityType == null || activityType.trim().isEmpty())
                && fromDate == null && toDate == null;
        String status = cropSeason.getSeasonStatus();
        boolean openSeason = status == null || (!"completed".equalsIgnoreCase(status)
                && !"cancelled".equalsIgnoreCase(status)
                && !"archived".equalsIgnoreCase(status));
        if (page.isEmpty() && noFilter && openSeason && cropSeason.getFarm() != null) {
            Block defaultBlock = cropSeason.getPlot();
            if (defaultBlock == null && cropSeasonBlockRepository != null) {
                List<CropSeasonBlock> assigned = cropSeasonBlockRepository.findByCropSeasonIdWithDetails(cropSeason.getId());
                if (!assigned.isEmpty()) {
                    defaultBlock = assigned.get(0).getBlock();
                }
            }
            List<FarmingActivity> defaults = List.of(
                    buildDefaultActivity(cropSeason, defaultBlock, member, "Làm đất", "Làm đất & xử lý giá thể đầu vụ", "Vệ sinh đồng ruộng, kiểm tra pH đất và xử lý giá thể chuẩn VietGAP."),
                    buildDefaultActivity(cropSeason, defaultBlock, member, "Gieo cấy", "Gieo hạt / xuống giống mùa vụ", "Xuống giống theo mật độ khuyến cáo và kiểm tra tỷ lệ nảy mầm."),
                    buildDefaultActivity(cropSeason, defaultBlock, member, "Bón phân", "Bón phân & bổ sung dinh dưỡng định kỳ", "Cung cấp dinh dưỡng hữu cơ vi sinh và khoáng đa trung vi lượng cho cây trồng."),
                    buildDefaultActivity(cropSeason, defaultBlock, member, "Tưới nước", "Tưới nước & duy trì độ ẩm", "Tưới tiêu kiểm soát độ ẩm đất và EC/pH nguồn nước tưới."),
                    buildDefaultActivity(cropSeason, defaultBlock, member, "Phun thuốc", "Phòng trừ sâu bệnh bằng chế phẩm sinh học", "Phun chế phẩm sinh học phòng ngừa sâu bệnh hại và tuân thủ thời gian cách ly PHI.")
            );
            List<FarmingActivity> savedDefaults = farmingActivityRepository.saveAll(defaults);
            if (savedDefaults != null && !savedDefaults.isEmpty()) {
                page = farmingActivityRepository.findAll(spec, pageable);
            }
        }
        return PageResp.map(page, act -> mapToSummaryResponse(act, member));
    }

    private FarmingActivity buildDefaultActivity(CropSeason cropSeason, Block block, FarmMember member, String type, String name, String desc) {
        FarmingActivity act = new FarmingActivity();
        act.setFarm(cropSeason.getFarm());
        act.setCropSeason(cropSeason);
        act.setBlock(block);
        if (block != null && block.getZone() != null) {
            act.setZone(block.getZone());
        }
        act.setCreatedBy(member != null ? member.getUser() : null);
        act.setActivityType(type);
        act.setActivityName(name);
        act.setStartedAt(LocalDateTime.now());
        act.setDescription(desc);
        return act;
    }

    @Override
    @Transactional
    public FarmingActivityResp updateActivity(Integer currentUserId, Integer activityId, UpdateFarmingActivityReq request) {
        FarmingActivity activity = farmingActivityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhật ký canh tác không tồn tại."));

        CropSeason cropSeason = activity.getCropSeason();
        if (cropSeason != null && ("completed".equalsIgnoreCase(cropSeason.getSeasonStatus()) || "cancelled".equalsIgnoreCase(cropSeason.getSeasonStatus()))) {
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

        String desc = request.description() != null ? request.description().trim() : "";
        String resNotes = request.resultNotes() != null ? request.resultNotes().trim() : "";
        if (!desc.isEmpty()) {
            activity.setDescription(desc);
        } else if (!resNotes.isEmpty()) {
            activity.setResultNotes(resNotes);
        }

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

        return mapToResponse(savedActivity, workerResponses, supervisor);
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

        return mapToResponse(activity, workerResponses, member);
    }

    private FarmingActivityResp mapToResponse(FarmingActivity act, List<ActivityWorkerResp> workers, FarmMember supervisor) {
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

    private FarmingActivitySummaryResp mapToSummaryResponse(FarmingActivity act, FarmMember supervisor) {
        List<ActivityWorker> workers = activityWorkerRepository.findByFarmingActivityId(act.getId());
        Block block = act.getBlock();

        return new FarmingActivitySummaryResp(
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
                workers.size(),
                act.getStartedAt()
        );
    }
}
