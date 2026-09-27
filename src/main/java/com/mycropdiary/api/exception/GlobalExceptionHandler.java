package com.mycropdiary.api.exception;

import com.mycropdiary.api.dto.common.ApiResponse;
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
    ApiResponse<Void> handleNotFound(ResourceNotFoundException exception) {
        return ApiResponse.error(exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ApiResponse<Void> handleAccessDenied(AccessDeniedException exception) {
        return ApiResponse.error(exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResponse<Void> handleBadRequest(RuntimeException exception) {
        return ApiResponse.error(exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResponse<Void> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("Dữ liệu không hợp lệ");
        return ApiResponse.error(message);
    }

    // [AI_CHANGE] Root cause: AuthService ném BadRequestException cho các lỗi nghiệp vụ (email trùng, OTP sai)
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResponse<Void> handleBadRequest(BadRequestException exception) {
        return ApiResponse.error(exception.getMessage());
    }

    // [AI_CHANGE] Root cause: Spring Security ném exception khi sai mật khẩu hoặc tài khoản bị khóa
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    ApiResponse<Void> handleBadCredentials(BadCredentialsException exception) {
        return ApiResponse.error("Email hoặc mật khẩu không chính xác.");
    }

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ApiResponse<Void> handleDisabled(DisabledException exception) {
        return ApiResponse.error("Tài khoản chưa được kích hoạt. Vui lòng xác thực OTP qua email.");
    }

    @ExceptionHandler(LockedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ApiResponse<Void> handleLocked(LockedException exception) {
        return ApiResponse.error("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ApiResponse<Void> handleForbidden(ForbiddenException exception) {
        return ApiResponse.error(exception.getMessage());
    }

    // [AI_CHANGE] Bắt mọi exception không xác định để không lộ stack trace ra client
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ApiResponse<Void> handleGeneral(Exception exception) {
        log.error("Unhandled exception: ", exception);
        return ApiResponse.error("Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.");
    }
}
