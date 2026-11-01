package com.mycropdiary.api.dto.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Yêu cầu ghi nhận chi phí sản xuất (Record Expense)")
public record CreateExpenseReq(
        @Schema(description = "Danh mục chi phí (Materials & Fertilizers, Labor, Utilities & Power, Machinery & Fuel, Pest Control, Packaging, Other)", example = "Materials & Fertilizers")
        @NotBlank(message = "Danh mục chi phí không được để trống")
        @Size(max = 80, message = "Danh mục chi phí tối đa 80 ký tự")
        String expenseCategory,

        @Schema(description = "Số tiền chi phí (VND)", example = "18500000.00")
        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "0.0", message = "Số tiền chi phí không được âm")
        BigDecimal amount,

        @Schema(description = "Ngày chi tiêu (mặc định hôm nay)")
        LocalDate expenseDate,

        @Schema(description = "ID mùa vụ áp dụng chi phí (tùy chọn)", example = "1")
        Integer cropSeasonId,

        @Schema(description = "ID công việc trang trại liên quan (tùy chọn)", example = "1")
        Integer farmTaskId,

        @Schema(description = "Mô tả / diễn giải khoản chi phí", example = "Mua phân bón hòa tan NPK và dung dịch dinh dưỡng khoáng đậm đặc")
        String description,

        @Schema(description = "Đường dẫn ảnh chụp hóa đơn / biên lai thanh toán")
        String receiptImageUrl
) {
}

