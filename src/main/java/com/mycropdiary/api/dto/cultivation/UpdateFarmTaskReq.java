package com.mycropdiary.api.dto.cultivation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record UpdateFarmTaskReq(
        @NotBlank(message = "Tên công việc không được để trống")
        @Size(max = 160, message = "Tên công việc tối đa 160 ký tự")
        String taskName,

        @Size(max = 60, message = "Phân loại công việc tối đa 60 ký tự")
        String taskType,

        @Size(max = 20, message = "Độ ưu tiên tối đa 20 ký tự")
        String priority, // low, normal, high, urgent

        @Size(max = 20, message = "Trạng thái tối đa 20 ký tự")
        String status, // planned, in_progress, completed, cancelled

        Integer cropSeasonId,

        Integer zoneId,

        Integer blockId,

        List<Integer> assignedWorkerIds,

        LocalDateTime startsAt,

        LocalDateTime dueAt,

        String description
) {}
