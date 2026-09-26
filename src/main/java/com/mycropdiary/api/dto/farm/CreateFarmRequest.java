package com.mycropdiary.api.dto.farm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO yêu cầu tạo mới trang trại.
 *
 * @param farmCode Mã trang trại (Duy nhất, tối đa 30 ký tự)
 * @param farmName Tên trang trại (Tối đa 200 ký tự)
 * @param addressLine Địa chỉ chi tiết
 * @param province Tỉnh/Thành phố
 * @param district Quận/Huyện
 * @param ward Phường/Xã
 * @param latitude Vĩ độ (-90 đến 90)
 * @param longitude Kinh độ (-180 đến 180)
 * @param totalAreaM2 Tổng diện tích tính bằng m2 (> 0)
 */
public record CreateFarmRequest(
        @NotBlank(message = "Farm code is required")
        @Size(max = 30, message = "Farm code must not exceed 30 characters")
        String farmCode,

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
        BigDecimal totalAreaM2
) {}
