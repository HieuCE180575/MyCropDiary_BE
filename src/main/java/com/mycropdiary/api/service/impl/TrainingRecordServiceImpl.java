package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.common.PageResp;
import com.mycropdiary.api.dto.training.*;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.cultivation.FarmWorker;
import com.mycropdiary.api.entity.cultivation.TrainingAttendee;
import com.mycropdiary.api.entity.cultivation.TrainingRecord;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.FarmWorkerRepository;
import com.mycropdiary.api.repository.cultivation.TrainingAttendeeRepository;
import com.mycropdiary.api.repository.cultivation.TrainingRecordRepository;
import com.mycropdiary.api.service.cultivation.TrainingRecordService;
import com.mycropdiary.api.util.FarmAccessGuard;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class TrainingRecordServiceImpl implements TrainingRecordService {

    private final TrainingRecordRepository trainingRecordRepository;
    private final TrainingAttendeeRepository attendeeRepository;
    private final FarmRepository farmRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final FarmWorkerRepository farmWorkerRepository;
    private final FarmAccessGuard accessGuard;

    public TrainingRecordServiceImpl(
            TrainingRecordRepository trainingRecordRepository,
            TrainingAttendeeRepository attendeeRepository,
            FarmRepository farmRepository,
            FarmMemberRepository farmMemberRepository,
            FarmWorkerRepository farmWorkerRepository,
            FarmAccessGuard accessGuard) {
        this.trainingRecordRepository = trainingRecordRepository;
        this.attendeeRepository = attendeeRepository;
        this.farmRepository = farmRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.farmWorkerRepository = farmWorkerRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    public TrainingRecordResp createTrainingRecord(Integer currentUserId, Integer farmId, CreateTrainingRecordReq request) {
        accessGuard.requireFarmAccess(currentUserId, farmId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Trang trại không tồn tại"));

        FarmMember organizer = null;
        if (request.organizedByMemberId() != null) {
            organizer = farmMemberRepository.findById(request.organizedByMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Thành viên tổ chức không tồn tại"));
            if (!organizer.getFarm().getId().equals(farmId)) {
                throw new BadRequestException("Người tổ chức phải thuộc trang trại này");
            }
        } else {
            organizer = farmMemberRepository.findByFarmIdAndUserIdAndStatus(farmId, currentUserId, "active").orElse(null);
        }

        TrainingRecord record = new TrainingRecord();
        record.setFarm(farm);
        record.setOrganizedBy(organizer);
        record.setTrainingTitle(request.trainingTitle().trim());
        record.setDescription(request.description() != null ? request.description().trim() : null);
        record.setTrainingDate(request.trainingDate());
        record.setImageUrl(request.imageUrl() != null ? request.imageUrl().trim() : null);

        TrainingRecord saved = trainingRecordRepository.save(record);

        if (request.attendees() != null && !request.attendees().isEmpty()) {
            for (TrainingAttendeeReq attReq : request.attendees()) {
                FarmWorker worker = farmWorkerRepository.findById(attReq.farmWorkerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Công nhân id=" + attReq.farmWorkerId() + " không tồn tại"));

                TrainingAttendee attendee = new TrainingAttendee(
                        saved,
                        worker,
                        attReq.attendanceStatus() != null ? attReq.attendanceStatus().trim().toLowerCase() : "attended",
                        attReq.notes() != null ? attReq.notes().trim() : null
                );
                saved.addAttendee(attendee);
            }
            saved = trainingRecordRepository.save(saved);
        }

        return toResponse(saved, true);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResp<TrainingRecordResp> getTrainingHistory(
            Integer currentUserId, Integer farmId, String keyword, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        accessGuard.requireFarmAccess(currentUserId, farmId);

        Specification<TrainingRecord> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("farm").get("id"), farmId));

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("trainingTitle")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(titleMatch, descMatch));
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("trainingDate"), fromDate));
            }

            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("trainingDate"), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<TrainingRecord> page = trainingRecordRepository.findAll(spec, pageable);
        return PageResp.map(page, r -> toResponse(r, false));
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingRecordResp getTrainingRecordDetails(Integer currentUserId, Integer trainingRecordId) {
        TrainingRecord record = trainingRecordRepository.findById(trainingRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ đào tạo không tồn tại"));

        accessGuard.requireFarmAccess(currentUserId, record.getFarm().getId());

        return toResponse(record, true);
    }

    @Override
    public TrainingRecordResp updateTrainingRecord(Integer currentUserId, Integer trainingRecordId, UpdateTrainingRecordReq request) {
        TrainingRecord record = trainingRecordRepository.findById(trainingRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ đào tạo không tồn tại"));

        accessGuard.requireFarmAccess(currentUserId, record.getFarm().getId());

        if (request.trainingTitle() != null && !request.trainingTitle().trim().isEmpty()) {
            record.setTrainingTitle(request.trainingTitle().trim());
        }

        if (request.description() != null) {
            record.setDescription(request.description().trim());
        }

        if (request.trainingDate() != null) {
            record.setTrainingDate(request.trainingDate());
        }

        if (request.imageUrl() != null) {
            record.setImageUrl(request.imageUrl().trim());
        }

        if (request.organizedByMemberId() != null) {
            FarmMember organizer = farmMemberRepository.findById(request.organizedByMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Thành viên tổ chức không tồn tại"));
            record.setOrganizedBy(organizer);
        }

        if (request.attendees() != null) {
            record.getAttendees().clear();
            trainingRecordRepository.flush();

            for (TrainingAttendeeReq attReq : request.attendees()) {
                FarmWorker worker = farmWorkerRepository.findById(attReq.farmWorkerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Công nhân id=" + attReq.farmWorkerId() + " không tồn tại"));

                TrainingAttendee attendee = new TrainingAttendee(
                        record,
                        worker,
                        attReq.attendanceStatus() != null ? attReq.attendanceStatus().trim().toLowerCase() : "attended",
                        attReq.notes() != null ? attReq.notes().trim() : null
                );
                record.addAttendee(attendee);
            }
        }

        TrainingRecord saved = trainingRecordRepository.save(record);
        return toResponse(saved, true);
    }

    @Override
    public void deleteTrainingRecord(Integer currentUserId, Integer trainingRecordId) {
        TrainingRecord record = trainingRecordRepository.findById(trainingRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ đào tạo không tồn tại"));

        accessGuard.requireFarmOwner(currentUserId, record.getFarm().getId());
        trainingRecordRepository.delete(record);
    }

    private TrainingRecordResp toResponse(TrainingRecord r, boolean includeAttendees) {
        List<TrainingAttendeeResp> attendeeList = null;
        if (includeAttendees) {
            attendeeList = r.getAttendees().stream().map(a -> {
                FarmWorker worker = a.getFarmWorker();
                String fullName = (worker != null && worker.getFarmMember() != null && worker.getFarmMember().getUser() != null)
                        ? worker.getFarmMember().getUser().getFullName() : null;
                return new TrainingAttendeeResp(
                        worker != null ? worker.getId() : null,
                        worker != null ? worker.getWorkerCode() : null,
                        fullName,
                        worker != null ? worker.getJobTitle() : null,
                        a.getAttendanceStatus(),
                        a.getNotes()
                );
            }).toList();
        }

        String organizerName = (r.getOrganizedBy() != null && r.getOrganizedBy().getUser() != null)
                ? r.getOrganizedBy().getUser().getFullName() : null;

        return new TrainingRecordResp(
                r.getId(),
                r.getFarm().getId(),
                r.getFarm().getFarmName(),
                r.getOrganizedBy() != null ? r.getOrganizedBy().getId() : null,
                organizerName,
                r.getTrainingTitle(),
                r.getDescription(),
                r.getTrainingDate(),
                r.getImageUrl(),
                r.getAttendees().size(),
                attendeeList,
                r.getCreatedAt()
        );
    }
}
