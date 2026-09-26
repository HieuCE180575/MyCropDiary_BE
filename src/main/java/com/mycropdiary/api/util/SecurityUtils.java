package com.mycropdiary.api.util;

import com.mycropdiary.api.entity.AppUser;
import com.mycropdiary.api.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {
    private final AppUserRepository appUserRepository;

    public SecurityUtils(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            String username = null;
            if (principal instanceof UserDetails userDetails) {
                username = userDetails.getUsername();
            } else if (principal instanceof String principalName) {
                username = principalName;
            }

            if (username != null) {
                return appUserRepository.findByEmailIgnoreCase(username)
                        .map(AppUser::getId)
                        .orElse(1L);
            }
        }
        return 1L; // Default fallback for dev/testing skeleton
    }
}
