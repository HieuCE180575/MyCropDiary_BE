package com.mycropdiary.api.dto.farmregistration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu gửi đơn đăng ký trang trại mới từ phía người dùng.
 *
 * @param farmName Tên trang trại (bắt buộc, tối đa 200 ký tự)
 * @param addressLine Địa chỉ chi tiết (bắt buộc, tối đa 300 ký tự)
 * @param province Tỉnh/Thành phố (tùy chọn, tối đa 100 ký tự)
 * @param district Quận/Huyện (tùy chọn, tối đa 100 ký tự)
 * @param ward Phường/Xã (tùy chọn, tối đa 100 ký tự)
 * @param description Mô tả trang trại (tùy chọn, tối đa 1000 ký tự)
 * @param documentUrl Đường dẫn tài liệu/giấy chứng nhận đính kèm (tùy chọn, dạng URL, tối đa 1000 ký tự)
 */
public record FarmRegistrationCreateRequest(
        @NotBlank(message = "Tên trang trại không được để trống")
        @Size(max = 200, message = "Tên trang trại không được vượt quá 200 ký tự")
        String farmName,

        @NotBlank(message = "Địa chỉ chi tiết không được để trống")
        @Size(max = 300, message = "Địa chỉ chi tiết không được vượt quá 300 ký tự")
        String addressLine,

        @Size(max = 100, message = "Tỉnh/Thành phố không được vượt quá 100 ký tự")
        String province,

        @Size(max = 100, message = "Quận/Huyện không được vượt quá 100 ký tự")
        String district,

        @Size(max = 100, message = "Phường/Xã không được vượt quá 100 ký tự")
        String ward,

        @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
        String description,

        @Size(max = 1000, message = "Đường dẫn tài liệu không được vượt quá 1000 ký tự")
        @Pattern(regexp = "^(https?://.*)?$", message = "Đường dẫn tài liệu phải là định dạng URL hợp lệ (bắt đầu bằng http:// hoặc https://)")
        String documentUrl
) {}
