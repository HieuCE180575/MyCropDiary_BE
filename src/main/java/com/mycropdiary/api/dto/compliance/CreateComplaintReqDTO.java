package com.mycropdiary.api.dto.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateComplaintReqDTO(
        Integer harvestRecordId,
        String lotCode,

        @NotBlank(message = "Tên khách hàng / đối tác không được để trống")
        String customerName,

        String customerPhone,

        @NotNull(message = "Ngày tiếp nhận không được để trống")
        LocalDate receivedDate,

        @NotBlank(message = "Phân loại vấn đề không được để trống")
        String issueType,

        @NotBlank(message = "Mô tả sự cố / khiếu nại không được để trống")
        String issueDescription,

        @NotBlank(message = "Mức độ nghiêm trọng không được để trống")
        String severity
) {}
