package com.mycropdiary.api.controller;

import com.mycropdiary.api.dto.auth.RegisterRequest;
import com.mycropdiary.api.dto.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        // TODO: persist the user, create OTP token, and call the email adapter.
        return ApiResponse.ok("Registration skeleton accepted", Map.of("email", request.email()));
    }

    @PostMapping("/verify-otp")
    public ApiResponse<Void> verifyOtp() {
        return ApiResponse.ok("OTP verification skeleton", null);
    }

    @PostMapping("/login")
    public ApiResponse<Void> login() {
        return ApiResponse.ok("JWT login skeleton", null);
    }
}
