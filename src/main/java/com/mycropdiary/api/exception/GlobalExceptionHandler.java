package com.mycropdiary.api.exception;

import com.mycropdiary.api.dto.common.ApiResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiResp<Void> handleNotFound(ResourceNotFoundException exception) {
        return ApiResp.error(exception.getMessage());
    }

    @ExceptionHandler(ResourceConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiResp<Void> handleConflict(ResourceConflictException exception) {
        return ApiResp.error(exception.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResp<Void> handleBusinessRule(BusinessRuleException exception) {
        return ApiResp.error(exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ApiResp<Void> handleAccessDenied(AccessDeniedException exception) {
        return ApiResp.error(exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResp<Void> handleBadRequest(RuntimeException exception) {
        return ApiResp.error(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResp<Void> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Dữ liệu không hợp lệ");
        return ApiResp.error(message);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResp<Void> handleMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException exception) {
        return ApiResp.error("Định dạng dữ liệu gửi lên không hợp lệ (ví dụ: ngày giờ, JSON sai cú pháp).");
    }

    // [AI_CHANGE] Root cause: AuthService ném BadRequestException cho các lỗi nghiệp vụ (email trùng, OTP sai)
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResp<Void> handleBadRequest(BadRequestException exception) {
        return ApiResp.error(exception.getMessage());
    }

    // [AI_CHANGE] Root cause: Spring Security ném exception khi sai mật khẩu hoặc tài khoản bị khóa
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ApiResp<Void> handleBadCredentials(BadCredentialsException exception) {
        return ApiResp.error("Email hoặc mật khẩu không chính xác.");
    }

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ApiResp<Void> handleDisabled(DisabledException exception) {
        return ApiResp.error("Tài khoản chưa được kích hoạt. Vui lòng xác thực OTP qua email.");
    }

    @ExceptionHandler(LockedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ApiResp<Void> handleLocked(LockedException exception) {
        return ApiResp.error("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ApiResp<Void> handleForbidden(ForbiddenException exception) {
        return ApiResp.error(exception.getMessage());
    }

    // [AI_CHANGE] Bắt mọi exception không xác định để không lộ stack trace ra client
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ApiResp<Void> handleGeneral(Exception exception) {
        log.error("Unhandled exception: ", exception);
        return ApiResp.error("Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");
    }
}
