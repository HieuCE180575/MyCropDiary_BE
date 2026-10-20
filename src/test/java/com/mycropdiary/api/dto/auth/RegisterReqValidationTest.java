package com.mycropdiary.api.dto.auth;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterReqValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Mật khẩu hợp lệ khi đủ từ 8-100 ký tự, có chữ hoa, thường và ký tự đặc biệt")
    void testValidPassword() {
        RegisterReq req = new RegisterReq("Nguyen Van A", "test@gmail.com", "0912345678", "ValidPass123@");
        Set<ConstraintViolation<RegisterReq>> violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Pass1@",       // < 8 ký tự
            "lowercase123@", // thiếu chữ hoa
            "UPPERCASE123@", // thiếu chữ thường
            "Password12345", // thiếu ký tự đặc biệt
            ""              // rỗng
    })
    @DisplayName("Từ chối mật khẩu không thỏa mãn quy tắc mật khẩu an toàn")
    void testInvalidPasswords(String invalidPassword) {
        RegisterReq req = new RegisterReq("Nguyen Van A", "test@gmail.com", "0912345678", invalidPassword);
        Set<ConstraintViolation<RegisterReq>> violations = validator.validate(req);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("Từ chối mật khẩu quá 100 ký tự")
    void testPasswordTooLong() {
        String longPassword = "A@a" + "1".repeat(98); // 101 ký tự
        RegisterReq req = new RegisterReq("Nguyen Van A", "test@gmail.com", "0912345678", longPassword);
        Set<ConstraintViolation<RegisterReq>> violations = validator.validate(req);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }
}
