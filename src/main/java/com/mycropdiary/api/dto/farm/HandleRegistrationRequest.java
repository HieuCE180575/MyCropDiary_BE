package com.mycropdiary.api.dto.farm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// [AI_CHANGE] Root cause: UC-39 Admin cần gửi hành động duyệt/từ chối kèm lý do từ chối (nếu có)
// [AI_CHANGE] Mechanism: Validate action chỉ nhận APPROVED hoặc REJECTED; rejectionReason bắt buộc khi từ chối
public record HandleRegistrationRequest(
        @NotBlank(message = "Hành động xử lý không được để trống")
        @Pattern(regexp = "APPROVED|REJECTED", message = "Hành động phải là APPROVED hoặc REJECTED")
        String action,

        @Size(max = 1000, message = "Lý do từ chối tối đa 1000 ký tự")
        String rejectionReason
) {}
