package com.mycropdiary.api.dto.procurement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Yêu cầu cập nhật nhà cung cấp (Update Supplier)")
public record UpdateSupplierReq(
        @Schema(description = "Tên nhà cung cấp", example = "Công ty Nông Dược Xanh Đà Lạt")
        @NotBlank(message = "Tên nhà cung cấp không được để trống")
        @Size(max = 160, message = "Tên nhà cung cấp tối đa 160 ký tự")
        String supplierName,

        @Schema(description = "Người liên hệ / Đại diện", example = "Nguyễn Phi Hùng")
        String contactName,

        @Schema(description = "Số điện thoại", example = "0988123456")
        String phone,

        @Schema(description = "Địa chỉ email", example = "contact@nongduocxanh.vn")
        String email,

        @Schema(description = "Địa chỉ trụ sở / kho hàng", example = "KCN Lộc Phát, TP. Bảo Lộc, Lâm Đồng")
        String address,

        @Schema(description = "Ghi chú bổ sung", example = "Cung cấp phân bón hòa tan chuẩn VietGAP")
        String notes
) {
}
