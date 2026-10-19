package com.mycropdiary.api.util;

import com.mycropdiary.api.entity.User;
import com.mycropdiary.api.exception.ResourceNotFoundException;
import com.mycropdiary.api.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Tiện ích hỗ trợ trích xuất thông tin người dùng đang đăng nhập từ Spring Security Context.
 */
@Component
public class SecurityUtils {
    public static final Integer DEFAULT_FALLBACK_USER_ID = 1;

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<Integer> getCurrentUserIdOptional() {
        // 1. Check HTTP header X-User-Id for testing/development flexibility
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String headerUserId = request.getHeader("X-User-Id");
            if (headerUserId != null && !headerUserId.isBlank()) {
                try {
                    return Optional.of(Integer.valueOf(headerUserId.trim()));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // 2. Check SecurityContext Authentication
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Integer userId) {
                return Optional.of(userId);
            }
            String username = null;
            if (principal instanceof UserDetails userDetails) {
                username = userDetails.getUsername();
            } else if (principal instanceof String principalName) {
                username = principalName;
            }

            if (username != null) {
                try {
                    return Optional.of(Integer.valueOf(username));
                } catch (IllegalArgumentException ignored) {
                    return userRepository.findByEmailIgnoreCase(username).map(User::getId);
                }
            }
        }
        return Optional.empty();
    }

    public Integer getCurrentUserId() {
        return getCurrentUserIdOptional().orElse(DEFAULT_FALLBACK_USER_ID);
    }

    public Integer getRequiredCurrentUserId() {
        return getCurrentUserIdOptional().orElseThrow(() ->
                new AccessDeniedException("Yêu cầu xác thực tài khoản người dùng"));
    }

    public User getRequiredCurrentUser() {
        Integer userId = getRequiredCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng ID: " + userId));
    }
}
