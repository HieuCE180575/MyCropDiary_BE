package com.mycropdiary.api.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Yêu cầu cập nhật chi phí (Update Expense)")
public record UpdateExpenseReq(
        @Schema(description = "Danh mục chi phí cập nhật", example = "Labor")
        @Size(max = 80, message = "Danh mục chi phí tối đa 80 ký tự")
        String expenseCategory,

        @Schema(description = "Số tiền chi phí cập nhật (VND)", example = "4500000.00")
        @DecimalMin(value = "0.0", message = "Số tiền chi phí không được âm")
        BigDecimal amount,

        @Schema(description = "Ngày chi tiêu cập nhật")
        LocalDate expenseDate,

        @Schema(description = "ID mùa vụ áp dụng chi phí (tùy chọn)", example = "1")
        Integer cropSeasonId,

        @Schema(description = "ID công việc trang trại liên quan (tùy chọn)", example = "1")
        Integer farmTaskId,

        @Schema(description = "Mô tả / diễn giải khoản chi phí", example = "Chi phí công nhân giặt và xử lý giá thể xơ dừa")
        String description,

        @Schema(description = "Đường dẫn ảnh chụp hóa đơn / biên lai thanh toán")
        String receiptImageUrl
) {
}

