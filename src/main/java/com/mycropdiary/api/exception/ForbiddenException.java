package com.mycropdiary.api.exception;

// [AI_CHANGE] Root cause: Cần exception cho truy cập trang trại/tài nguyên không thuộc quyền (403)
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
