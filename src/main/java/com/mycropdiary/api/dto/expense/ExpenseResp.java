package com.mycropdiary.api.dto.expense;

import com.mycropdiary.api.entity.cultivation.Expense;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "Thông tin chi tiết khoản chi phí (Expense Details)")
public record ExpenseResp(
        Integer expenseId,
        Integer farmId,
        Integer cropSeasonId,
        String seasonName,
        Integer farmTaskId,
        String taskTitle,
        String expenseCategory,
        String description,
        BigDecimal amount,
        LocalDate expenseDate,
        Integer recordedByMemberId,
        String recordedByName,
        String receiptImageUrl,
        Instant createdAt
) {
    public static ExpenseResp fromEntity(Expense entity) {
        if (entity == null) return null;

        Integer farmId = entity.getFarm() != null ? entity.getFarm().getId() : null;
        Integer seasonId = entity.getCropSeason() != null ? entity.getCropSeason().getId() : null;
        String seasonName = entity.getCropSeason() != null ? entity.getCropSeason().getSeasonName() : null;

        Integer taskId = entity.getFarmTask() != null ? entity.getFarmTask().getId() : null;
        String taskTitle = entity.getFarmTask() != null ? entity.getFarmTask().getTaskName() : null;

        Integer memberId = entity.getRecordedBy() != null ? entity.getRecordedBy().getId() : null;
        String memberName = entity.getRecordedBy() != null && entity.getRecordedBy().getUser() != null
                ? entity.getRecordedBy().getUser().getFullName() : null;

        return new ExpenseResp(
                entity.getId(),
                farmId,
                seasonId,
                seasonName,
                taskId,
                taskTitle,
                entity.getExpenseCategory(),
                entity.getDescription(),
                entity.getAmount(),
                entity.getExpenseDate(),
                memberId,
                memberName,
                entity.getReceiptImageUrl(),
                entity.getCreatedAt()
        );
    }
}
