package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.cultivation.*;
import com.mycropdiary.api.entity.Block;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.Zone;
import com.mycropdiary.api.entity.cultivation.*;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.BlockRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.UserRepository;
import com.mycropdiary.api.repository.ZoneRepository;
import com.mycropdiary.api.repository.cultivation.*;
import com.mycropdiary.api.service.cultivation.FarmTaskService;
import com.mycropdiary.api.util.FarmAccessGuard;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class FarmTaskServiceImpl implements FarmTaskService {

    private final FarmTaskRepository farmTaskRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final FarmRepository farmRepository;
    private final UserRepository userRepository;
    private final FarmWorkerRepository farmWorkerRepository;
    private final CropSeasonRepository cropSeasonRepository;
    private final ZoneRepository zoneRepository;
    private final BlockRepository blockRepository;
    private final FarmAccessGuard farmAccessGuard;

    public FarmTaskServiceImpl(
            FarmTaskRepository farmTaskRepository,
            TaskAssignmentRepository taskAssignmentRepository,
            FarmRepository farmRepository,
            UserRepository userRepository,
            FarmWorkerRepository farmWorkerRepository,
            CropSeasonRepository cropSeasonRepository,
            ZoneRepository zoneRepository,
            BlockRepository blockRepository,
            FarmAccessGuard farmAccessGuard) {
        this.farmTaskRepository = farmTaskRepository;
        this.taskAssignmentRepository = taskAssignmentRepository;
        this.farmRepository = farmRepository;
        this.userRepository = userRepository;
        this.farmWorkerRepository = farmWorkerRepository;
        this.cropSeasonRepository = cropSeasonRepository;
        this.zoneRepository = zoneRepository;
        this.blockRepository = blockRepository;
        this.farmAccessGuard = farmAccessGuard;
    }

    @Override
    public PageResp<FarmTaskResp> getFarmTasks(
            Integer userId,
            Integer farmId,
            Integer cropSeasonId,
            Integer zoneId,
            String status,
            String priority,
            String keyword,
            Pageable pageable) {

        farmAccessGuard.requireFarmMember(userId, farmId);

        // Auto-seed default tasks if farm has 0 tasks
        boolean noFilter = cropSeasonId == null && zoneId == null
                && (status == null || status.isBlank() || "all".equalsIgnoreCase(status.trim()))
                && (priority == null || priority.isBlank() || "all".equalsIgnoreCase(priority.trim()))
                && (keyword == null || keyword.isBlank());

        if (!farmTaskRepository.existsByFarmId(farmId) && noFilter) {
            seedDefaultTasksForFarm(userId, farmId);
        }

        Specification<FarmTask> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("farm").get("id"), farmId));

            if (cropSeasonId != null) {
                predicates.add(cb.equal(root.get("cropSeason").get("id"), cropSeasonId));
            }
            if (zoneId != null) {
                predicates.add(cb.equal(root.get("zone").get("id"), zoneId));
            }
            if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status.trim())) {
                predicates.add(cb.equal(cb.lower(root.get("status")), status.trim().toLowerCase()));
            }
            if (priority != null && !priority.isBlank() && !"all".equalsIgnoreCase(priority.trim())) {
                predicates.add(cb.equal(cb.lower(root.get("priority")), priority.trim().toLowerCase()));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("taskName")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(nameMatch, descMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<FarmTask> page = farmTaskRepository.findAll(spec, pageable);
        return PageResp.map(page, this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FarmTaskResp getTaskById(Integer userId, Integer taskId) {
        FarmTask task = farmTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Công việc không tồn tại ID: " + taskId));

        farmAccessGuard.requireFarmMember(userId, task.getFarm().getId());
        return mapToResponse(task);
    }

    @Override
    public FarmTaskResp createTask(Integer userId, Integer farmId, CreateFarmTaskReq request) {
        farmAccessGuard.requireFarmMember(userId, farmId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Trang trại không tồn tại ID: " + farmId));

        if (request.startsAt() != null && request.dueAt() != null && request.dueAt().isBefore(request.startsAt())) {
            throw new BadRequestException("Hạn hoàn thành không thể trước thời gian bắt đầu.");
        }

        User creator = userRepository.findById(userId).orElse(null);

        FarmTask task = new FarmTask();
        task.setFarm(farm);
        task.setTaskName(request.taskName().trim());
        task.setTaskType(cleanText(request.taskType()));
        task.setPriority(normalizePriority(request.priority()));
        task.setStatus(normalizeStatus(request.status()));
        task.setStartsAt(request.startsAt());
        task.setDueAt(request.dueAt());
        task.setDescription(cleanText(request.description()));
        task.setCreatedBy(creator);

        if (request.cropSeasonId() != null) {
            CropSeason season = cropSeasonRepository.findById(request.cropSeasonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ không tồn tại ID: " + request.cropSeasonId()));
            if (!season.getFarm().getId().equals(farmId)) {
                throw new BadRequestException("Mùa vụ không thuộc trang trại này.");
            }
            task.setCropSeason(season);
        }

        if (request.zoneId() != null) {
            Zone zone = zoneRepository.findById(request.zoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khu vực không tồn tại ID: " + request.zoneId()));
            if (!zone.getFarm().getId().equals(farmId)) {
                throw new BadRequestException("Khu vực không thuộc trang trại này.");
            }
            task.setZone(zone);
        }

        if (request.blockId() != null) {
            Block block = blockRepository.findById(request.blockId())
                    .orElseThrow(() -> new ResourceNotFoundException("Thửa đất không tồn tại ID: " + request.blockId()));
            task.setBlock(block);
            if (task.getZone() == null && block.getZone() != null) {
                task.setZone(block.getZone());
            }
        }

        FarmTask savedTask = farmTaskRepository.save(task);

        if (request.assignedWorkerIds() != null && !request.assignedWorkerIds().isEmpty()) {
            List<FarmWorker> workers = farmWorkerRepository.findByIdInAndFarmId(request.assignedWorkerIds(), farmId);
            for (FarmWorker worker : workers) {
                TaskAssignment assignment = new TaskAssignment(savedTask, worker, creator, "assigned");
                savedTask.addAssignment(assignment);
            }
            savedTask = farmTaskRepository.save(savedTask);
        }

        return mapToResponse(savedTask);
    }

    @Override
    public FarmTaskResp updateTask(Integer userId, Integer taskId, UpdateFarmTaskReq request) {
        FarmTask task = farmTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Công việc không tồn tại ID: " + taskId));

        Integer farmId = task.getFarm().getId();
        farmAccessGuard.requireFarmMember(userId, farmId);

        if (request.startsAt() != null && request.dueAt() != null && request.dueAt().isBefore(request.startsAt())) {
            throw new BadRequestException("Hạn hoàn thành không thể trước thời gian bắt đầu.");
        }

        task.setTaskName(request.taskName().trim());
        task.setTaskType(cleanText(request.taskType()));
        task.setPriority(normalizePriority(request.priority()));
        task.setStatus(normalizeStatus(request.status()));
        task.setStartsAt(request.startsAt());
        task.setDueAt(request.dueAt());
        task.setDescription(cleanText(request.description()));

        if (request.cropSeasonId() != null) {
            CropSeason season = cropSeasonRepository.findById(request.cropSeasonId())
                    .orElseThrow(() -> new ResourceNotFoundException("Mùa vụ không tồn tại ID: " + request.cropSeasonId()));
            task.setCropSeason(season);
        } else {
            task.setCropSeason(null);
        }

        if (request.zoneId() != null) {
            Zone zone = zoneRepository.findById(request.zoneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khu vực không tồn tại ID: " + request.zoneId()));
            task.setZone(zone);
        } else {
            task.setZone(null);
        }

        if (request.blockId() != null) {
            Block block = blockRepository.findById(request.blockId())
                    .orElseThrow(() -> new ResourceNotFoundException("Thửa đất không tồn tại ID: " + request.blockId()));
            task.setBlock(block);
        } else {
            task.setBlock(null);
        }

        // Update assignments
        task.getAssignments().clear();
        taskAssignmentRepository.deleteByFarmTaskId(taskId);
        taskAssignmentRepository.flush();

        if (request.assignedWorkerIds() != null && !request.assignedWorkerIds().isEmpty()) {
            User currentUser = userRepository.findById(userId).orElse(null);
            List<FarmWorker> workers = farmWorkerRepository.findByIdInAndFarmId(request.assignedWorkerIds(), farmId);
            for (FarmWorker worker : workers) {
                TaskAssignment assignment = new TaskAssignment(task, worker, currentUser, "assigned");
                task.addAssignment(assignment);
            }
        }

        FarmTask updated = farmTaskRepository.save(task);
        return mapToResponse(updated);
    }

    @Override
    public FarmTaskResp updateTaskStatus(Integer userId, Integer taskId, UpdateFarmTaskStatusReq request) {
        FarmTask task = farmTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Công việc không tồn tại ID: " + taskId));

        farmAccessGuard.requireFarmMember(userId, task.getFarm().getId());
        task.setStatus(normalizeStatus(request.status()));

        FarmTask saved = farmTaskRepository.save(task);
        return mapToResponse(saved);
    }

    @Override
    public void deleteTask(Integer userId, Integer taskId) {
        FarmTask task = farmTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Công việc không tồn tại ID: " + taskId));

        farmAccessGuard.requireFarmMember(userId, task.getFarm().getId());
        farmTaskRepository.delete(task);
    }

    private void seedDefaultTasksForFarm(Integer userId, Integer farmId) {
        Optional<Farm> farmOpt = farmRepository.findById(farmId);
        if (farmOpt.isEmpty()) return;
        Farm farm = farmOpt.get();
        User creator = userRepository.findById(userId).orElse(null);

        List<FarmWorker> workers = farmWorkerRepository.findByFarmId(farmId);

        List<FarmTask> defaultTasks = List.of(
                buildTemplateTask(farm, creator, "Pha chế dịch dinh dưỡng và kiểm tra hệ thống tưới nhỏ giọt",
                        "Đo EC và pH bồn chứa dinh dưỡng trước khi bật bơm tự động", "Tưới tiêu & Dinh dưỡng", "high", "planned",
                        LocalDateTime.now().plusDays(1)),
                buildTemplateTask(farm, creator, "Phun chế phẩm vi sinh ngừa bọ trĩ luống A2-A4",
                        "Dùng Neem Oil nồng độ 0.2%, phun lúc chiều râm mát", "Bảo vệ thực vật", "urgent", "in_progress",
                        LocalDateTime.now().plusDays(2)),
                buildTemplateTask(farm, creator, "Cắt tỉa nhánh phụ và cố định dây leo giàn dưa",
                        "Giữ lại 1 nhánh chính và tỉa bỏ các chồi nách dưới mắt lá số 5", "Chăm sóc sinh trưởng", "normal", "in_progress",
                        LocalDateTime.now().plusDays(3)),
                buildTemplateTask(farm, creator, "Lấy mẫu đất và nước tưới gửi xét nghiệm định kỳ VietGAP",
                        "Lấy mẫu ngẫu nhiên tại 5 điểm giao dịch, gửi trung tâm kiểm nghiệm Vinacert", "Tuân thủ VietGAP", "high", "completed",
                        LocalDateTime.now().minusDays(1))
        );

        for (int i = 0; i < defaultTasks.size(); i++) {
            FarmTask saved = farmTaskRepository.save(defaultTasks.get(i));
            if (!workers.isEmpty()) {
                FarmWorker w = workers.get(i % workers.size());
                TaskAssignment assignment = new TaskAssignment(saved, w, creator, "assigned");
                saved.addAssignment(assignment);
                farmTaskRepository.save(saved);
            }
        }
    }

    private FarmTask buildTemplateTask(Farm farm, User creator, String name, String desc, String type, String priority, String status, LocalDateTime dueAt) {
        FarmTask task = new FarmTask();
        task.setFarm(farm);
        task.setCreatedBy(creator);
        task.setTaskName(name);
        task.setDescription(desc);
        task.setTaskType(type);
        task.setPriority(priority);
        task.setStatus(status);
        task.setStartsAt(LocalDateTime.now());
        task.setDueAt(dueAt);
        return task;
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) return "normal";
        String p = priority.trim().toLowerCase();
        if (p.contains("urgent") || p.contains("khẩn")) return "urgent";
        if (p.contains("high") || p.contains("cao")) return "high";
        if (p.contains("low") || p.contains("thấp")) return "low";
        return "normal";
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "planned";
        String s = status.trim().toLowerCase();
        if (s.contains("doing") || s.contains("progress") || s.contains("đang")) return "in_progress";
        if (s.contains("done") || s.contains("complete") || s.contains("hoàn")) return "completed";
        if (s.contains("cancel") || s.contains("hủy")) return "cancelled";
        return "planned";
    }

    private String cleanText(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private FarmTaskResp mapToResponse(FarmTask t) {
        CropSeason cs = t.getCropSeason();
        Zone z = t.getZone();
        Block b = t.getBlock();
        User u = t.getCreatedBy();

        List<TaskWorkerResp> assignees = new ArrayList<>();
        if (t.getAssignments() != null) {
            for (TaskAssignment a : t.getAssignments()) {
                FarmWorker fw = a.getFarmWorker();
                if (fw != null) {
                    assignees.add(new TaskWorkerResp(
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

        return new FarmTaskResp(
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
