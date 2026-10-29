package com.mycropdiary.api.dto.cultivation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateCropSeasonReq(
        @NotBlank(message = "Tên mùa vụ không được để trống")
        @Size(max = 120, message = "Tên mùa vụ không được vượt quá 120 ký tự")
        String seasonName,

        @NotNull(message = "Loại cây trồng không được để trống")
        Integer cropCategoryId,

        @NotNull(message = "Ngày bắt đầu mùa vụ không được để trống")
        LocalDate startDate,

        LocalDate endDate,

        @Size(max = 20, message = "Trạng thái không được vượt quá 20 ký tự")
        String seasonStatus, // planned, active

        String notes
) {
}

