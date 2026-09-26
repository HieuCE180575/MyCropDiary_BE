package com.mycropdiary.api.dto.farm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO yêu cầu cập nhật thông tin trang trại.
 *
 * @param farmName Tên trang trại
 * @param addressLine Địa chỉ chi tiết
 * @param province Tỉnh/Thành phố
 * @param district Quận/Huyện
 * @param ward Phường/Xã
 * @param latitude Vĩ độ
 * @param longitude Kinh độ
 * @param totalAreaM2 Tổng diện tích (m2)
 * @param status Trạng thái (ACTIVE, INACTIVE)
 */
public record UpdateFarmRequest(
        @NotBlank(message = "Farm name is required")
        @Size(max = 200, message = "Farm name must not exceed 200 characters")
        String farmName,

        @Size(max = 300, message = "Address line must not exceed 300 characters")
        String addressLine,

        String province,
        String district,
        String ward,

        BigDecimal latitude,
        BigDecimal longitude,

        @Positive(message = "Total area must be positive")
        BigDecimal totalAreaM2,

        String status
) {}
