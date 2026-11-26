package com.mycropdiary.api.service.impl;

import com.mycropdiary.api.dto.worker.CreateWorkerReqDTO;
import com.mycropdiary.api.dto.worker.UpdateWorkerReqDTO;
import com.mycropdiary.api.dto.worker.FarmWorkerRespDTO;
import com.mycropdiary.api.entity.Farm;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.cultivation.FarmWorker;
import com.mycropdiary.api.exception.BadRequestException;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.mapper.FarmWorkerMapper;
import com.mycropdiary.api.repository.FarmMemberRepository;
import com.mycropdiary.api.repository.FarmRepository;
import com.mycropdiary.api.repository.cultivation.FarmWorkerRepository;
import com.mycropdiary.api.service.cultivation.FarmWorkerService;
import com.mycropdiary.api.util.FarmAccessGuard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FarmWorkerServiceImpl implements FarmWorkerService {

    private final FarmWorkerRepository farmWorkerRepository;
    private final FarmMemberRepository farmMemberRepository;
    private final FarmAccessGuard accessGuard;
    private final FarmWorkerMapper farmWorkerMapper = new FarmWorkerMapper();

    @Autowired(required = false)
    private FarmRepository farmRepository;

    public FarmWorkerServiceImpl(FarmWorkerRepository farmWorkerRepository,
                                 FarmMemberRepository farmMemberRepository,
                                 FarmAccessGuard accessGuard) {
        this.farmWorkerRepository = farmWorkerRepository;
        this.farmMemberRepository = farmMemberRepository;
        this.accessGuard = accessGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmWorkerRespDTO> getWorkersByFarm(Integer userId, Integer farmId, String status, String keyword) {
        accessGuard.requireFarmAccess(userId, farmId);
        List<FarmWorker> workers = farmWorkerRepository.findByFarmId(farmId);

        if (status != null && !status.isBlank()) {
            String s = status.trim().toLowerCase();
            workers = workers.stream()
                    .filter(w -> w.getEmploymentStatus() != null && w.getEmploymentStatus().equalsIgnoreCase(s))
                    .toList();
        }

        if (keyword != null && !keyword.isBlank()) {
            String q = keyword.trim().toLowerCase();
            workers = workers.stream()
                    .filter(w -> (w.getFullName() != null && w.getFullName().toLowerCase().contains(q))
                            || (w.getWorkerCode() != null && w.getWorkerCode().toLowerCase().contains(q))
                            || (w.getJobTitle() != null && w.getJobTitle().toLowerCase().contains(q)))
                    .toList();
        }

        return farmWorkerMapper.toResponseList(workers);
    }

    @Override
    @Transactional(readOnly = true)
    public FarmWorkerRespDTO getWorkerById(Integer userId, Integer farmId, Integer workerId) {
        accessGuard.requireFarmAccess(userId, farmId);
        FarmWorker worker = farmWorkerRepository.findByIdAndFarmMemberFarmId(workerId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ công nhân ID=" + workerId + " không tồn tại."));
        return farmWorkerMapper.toResponse(worker);
    }

    @Override
    public FarmWorkerRespDTO createWorker(Integer userId, Integer farmId, CreateWorkerReqDTO req) {
        accessGuard.requireFarmAccess(userId, farmId);

        FarmMember member = null;
        Farm farm = null;
        if (req.farmMemberId() != null && req.farmMemberId() > 0) {
            member = farmMemberRepository.findById(req.farmMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Thành viên trang trại ID=" + req.farmMemberId() + " không tồn tại."));

            if (!member.getFarm().getId().equals(farmId)) {
                throw new BadRequestException("Thành viên không thuộc trang trại này.");
            }

            if (farmWorkerRepository.findByFarmMemberId(req.farmMemberId()).isPresent()) {
                throw new BadRequestException("Thành viên này đã có hồ sơ công nhân lao động.");
            }
            farm = member.getFarm();
        } else {
            if (req.fullName() == null || req.fullName().isBlank()) {
                throw new BadRequestException("Vui lòng nhập họ tên công nhân hoặc chọn thành viên trang trại.");
            }
            if (farmRepository != null) {
                farm = farmRepository.findById(farmId)
                        .orElseThrow(() -> new ResourceNotFoundException("Trang trại ID=" + farmId + " không tồn tại."));
            }
        }

        String code = req.workerCode().trim();
        if (farmWorkerRepository.existsByFarmMemberFarmIdAndWorkerCodeIgnoreCase(farmId, code)) {
            throw new BadRequestException("Mã công nhân '" + code + "' đã tồn tại trong trang trại.");
        }

        String resolvedFullName = (req.fullName() != null && !req.fullName().isBlank())
                ? req.fullName().trim()
                : (member != null && member.getUser() != null ? member.getUser().getFullName() : null);
        String resolvedPhone = (req.phoneNumber() != null && !req.phoneNumber().isBlank())
                ? req.phoneNumber().trim()
                : (member != null && member.getUser() != null ? member.getUser().getPhone() : null);

        FarmWorker worker = new FarmWorker(
                farm,
                member,
                resolvedFullName,
                resolvedPhone,
                code,
                req.jobTitle() != null ? req.jobTitle().trim() : null,
                req.employmentStatus() != null ? req.employmentStatus().trim() : "active",
                req.hiredAt(),
                req.notes() != null ? req.notes().trim() : null
        );

        FarmWorker saved = farmWorkerRepository.save(worker);
        return farmWorkerMapper.toResponse(saved);
    }

    @Override
    public FarmWorkerRespDTO updateWorker(Integer userId, Integer farmId, Integer workerId, UpdateWorkerReqDTO req) {
        accessGuard.requireFarmAccess(userId, farmId);

        FarmWorker worker = farmWorkerRepository.findByIdAndFarmMemberFarmId(workerId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ công nhân ID=" + workerId + " không tồn tại."));

        String code = req.workerCode().trim();
        if (!worker.getWorkerCode().equalsIgnoreCase(code)
                && farmWorkerRepository.existsByFarmMemberFarmIdAndWorkerCodeIgnoreCase(farmId, code)) {
            throw new BadRequestException("Mã công nhân '" + code + "' đã tồn tại trong trang trại.");
        }

        if (req.fullName() != null && !req.fullName().isBlank()) {
            worker.setFullName(req.fullName().trim());
        }
        if (req.phoneNumber() != null) {
            worker.setPhoneNumber(req.phoneNumber().isBlank() ? null : req.phoneNumber().trim());
        }
        worker.setWorkerCode(code);
        worker.setJobTitle(req.jobTitle() != null ? req.jobTitle().trim() : null);
        if (req.employmentStatus() != null) {
            worker.setEmploymentStatus(req.employmentStatus().trim());
        }
        worker.setHiredAt(req.hiredAt());
        worker.setNotes(req.notes() != null ? req.notes().trim() : null);

        FarmWorker saved = farmWorkerRepository.save(worker);
        return farmWorkerMapper.toResponse(saved);
    }

    @Override
    public void deleteWorker(Integer userId, Integer farmId, Integer workerId) {
        accessGuard.requireFarmAccess(userId, farmId);

        FarmWorker worker = farmWorkerRepository.findByIdAndFarmMemberFarmId(workerId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Hồ sơ công nhân ID=" + workerId + " không tồn tại."));

        farmWorkerRepository.delete(worker);
    }
}

