package com.mycropdiary.api.mapper;

import com.mycropdiary.api.dto.auth.AuthRespDTO;
import com.mycropdiary.api.dto.auth.RegisterAccReqDTO;
import com.mycropdiary.api.entity.User;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public AuthRespDTO toResponse(User user, String accessToken, String refreshToken) {
        if (user == null) {
            return null;
        }
        return AuthRespDTO.of(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getSystemRole()
        );
    }

    public User toEntity(RegisterAccReqDTO req, String encodedPassword) {
        if (req == null) {
            return null;
        }
        return new User(
                req.email().trim().toLowerCase(),
                encodedPassword,
                req.fullName().trim(),
                req.phoneNumber() != null ? req.phoneNumber().trim() : null,
                "user",
                "pending"
        );
    }
}
