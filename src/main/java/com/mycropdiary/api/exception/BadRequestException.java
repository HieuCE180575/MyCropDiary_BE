package com.mycropdiary.api.exception;

// [AI_CHANGE] Root cause: Cần exception cho các lỗi nghiệp vụ (email đã tồn tại, OTP sai, v.v.)
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
