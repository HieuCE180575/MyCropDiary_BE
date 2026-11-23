package com.mycropdiary.api.dto.staff;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;


public record AddStaffReq(
        @Email(message = "Email không đúng định dạng")
        String email,

        Integer userId,

        String farmRole,

        @Size(max = 100, message = "Chức danh tối đa 100 ký tự")
        String jobTitle
) {
    @JsonIgnore
    @AssertTrue(message = "Vui lòng cung cấp email hoặc userId của nhân viên")
    public boolean isIdentifierProvided() {
        return (email != null && !email.isBlank()) || userId != null;
    }
}
