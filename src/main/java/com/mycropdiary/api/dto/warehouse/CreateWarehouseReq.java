package com.mycropdiary.api.dto.warehouse;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWarehouseReq(
        @NotBlank(message = "Tên kho hàng không được để trống")
        @Size(max = 120, message = "Tên kho hàng tối đa 120 ký tự")
        String warehouseName,

        @Size(max = 50, message = "Loại kho hàng tối đa 50 ký tự")
        String warehouseType,

        String address,

        String imageUrl
) {
    public CreateWarehouseReq(String warehouseName, String address, String imageUrl) {
        this(warehouseName, "kho_chung", address, imageUrl);
    }
}

