package com.mycropdiary.api.dto.worker;

import com.mycropdiary.api.entity.cultivation.FarmWorker;
import java.time.LocalDate;

public record FarmWorkerResp(
        Integer workerId,
        Integer farmId,
        Integer farmMemberId,
        Integer userId,
        String fullName,
        String phoneNumber,
        String email,
        String workerCode,
        String jobTitle,
        String employmentStatus,
        LocalDate hiredAt,
        String notes
) {
    public static FarmWorkerResp fromEntity(FarmWorker w) {
        Integer farmId = w.getFarm() != null ? w.getFarm().getId() : null;
        Integer memberId = w.getFarmMember() != null ? w.getFarmMember().getId() : null;
        Integer userId = (w.getFarmMember() != null && w.getFarmMember().getUser() != null)
                ? w.getFarmMember().getUser().getId() : null;
        String fullName = w.getFullName();
        String phone = w.getPhoneNumber();
        String email = (w.getFarmMember() != null && w.getFarmMember().getUser() != null)
                ? w.getFarmMember().getUser().getEmail() : null;

        return new FarmWorkerResp(
                w.getId(),
                farmId,
                memberId,
                userId,
                fullName,
                phone,
                email,
                w.getWorkerCode(),
                w.getJobTitle(),
                w.getEmploymentStatus(),
                w.getHiredAt(),
                w.getNotes()
        );
    }
}

