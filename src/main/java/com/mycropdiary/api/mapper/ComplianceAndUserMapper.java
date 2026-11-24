package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.admin.UserResp;
import com.mycropdiary.api.dto.training.TrainingAttendeeResp;
import com.mycropdiary.api.dto.training.TrainingRecordResp;
import com.mycropdiary.api.dto.vietgap.VietGapCriterionResp;
import com.mycropdiary.api.dto.staff.FarmMemberResp;
import com.mycropdiary.api.entity.FarmMember;
import com.mycropdiary.api.entity.StaffZoneAssignment;
import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.entity.cultivation.AssessmentCriterion;
import com.mycropdiary.api.entity.cultivation.FarmWorker;
import com.mycropdiary.api.entity.cultivation.TrainingRecord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Lớp chuyển đổi dữ liệu (Mapper) cho Nhân sự (FarmStaff), Đào tạo (Training),
 * Tiêu chí VietGAP (AssessmentCriterion) và Quản trị người dùng (AdminUser).
 */
@Component
public class ComplianceAndUserMapper {

    public FarmMemberResp toStaffMemberResponse(FarmMember member, int activeAssignmentCount) {
        return new FarmMemberResp(
                member.getId(),
                member.getFarm().getId(),
                member.getFarm().getFarmName(),
                member.getUser().getId(),
                member.getUser().getFullName(),
                member.getUser().getEmail(),
                member.getUser().getPhone(),
                member.getMemberRole(),
                member.getJobTitle(),
                member.getJoinedAt(),
                null,
                member.getMembershipStatus(),
                activeAssignmentCount
        );
    }

    public TrainingRecordResp toTrainingRecordResponse(TrainingRecord r, boolean includeAttendees) {
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

    public VietGapCriterionResp toVietGapCriterionResponse(AssessmentCriterion c, String baseDesc, String method) {
        return new VietGapCriterionResp(
                c.getId(),
                c.getFarm().getId(),
                c.getFarm().getFarmName(),
                c.getCriterionName(),
                baseDesc,
                c.getMaxScore(),
                method,
                "TCVN 11892-1:2017",
                c.getIsActive()
        );
    }

    public UserResp toEnrichedUserResp(User user, List<FarmMember> memberships, List<StaffZoneAssignment> zoneAssignments) {
        String farmsSummary = memberships.isEmpty()
                ? "Chưa tham gia trang trại"
                : memberships.stream()
                        .filter(m -> m.getFarm() != null)
                        .map(m -> {
                            String roleLabel = m.isOwner() ? "Chủ trại" : "Nhân sự";
                            return m.getFarm().getFarmName() + " (" + roleLabel + ")";
                        })
                        .collect(Collectors.joining(", "));

        String zonesSummary = zoneAssignments.isEmpty()
                ? "Chưa phụ trách phân khu"
                : zoneAssignments.stream()
                        .filter(a -> a.getZone() != null)
                        .map(a -> a.getZone().getZoneName() + (a.getZone().getFarm() != null ? " - " + a.getZone().getFarm().getFarmName() : ""))
                        .collect(Collectors.joining(", "));

        return UserResp.fromEntity(user, farmsSummary, zonesSummary);
    }
}
