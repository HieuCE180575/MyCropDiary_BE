package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.training.TrainingAttendeeRespDTO;
import com.mycropdiary.api.dto.training.TrainingRecordRespDTO;
import com.mycropdiary.api.entity.cultivation.FarmWorker;
import com.mycropdiary.api.entity.cultivation.TrainingRecord;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TrainingRecordMapper {

    public TrainingRecordRespDTO toTrainingRecordResponse(TrainingRecord r, boolean includeAttendees) {
        if (r == null) {
            return null;
        }
        List<TrainingAttendeeRespDTO> attendeeList = null;
        if (includeAttendees) {
            attendeeList = r.getAttendees().stream().map(a -> {
                FarmWorker worker = a.getFarmWorker();
                String fullName = (worker != null && worker.getFarmMember() != null && worker.getFarmMember().getUser() != null)
                        ? worker.getFarmMember().getUser().getFullName() : null;
                return new TrainingAttendeeRespDTO(
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

        return new TrainingRecordRespDTO(
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
