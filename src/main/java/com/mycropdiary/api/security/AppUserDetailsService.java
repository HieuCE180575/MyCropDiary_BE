package com.mycropdiary.api.security;

import com.mycropdiary.api.entity.AppUser;
import com.mycropdiary.api.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

// [AI_CHANGE] Root cause: Spring Security cần UserDetailsService để load thông tin người dùng từ DB
// [AI_CHANGE] Mechanism: Tìm AppUser theo email, map SystemRole sang GrantedAuthority với prefix ROLE_
@Service
public class AppUserDetailsService implements UserDetailsService {
    private final AppUserRepository appUserRepository;

    public AppUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản với email: " + email));

        return new User(
                appUser.getId().toString(),
                appUser.getPasswordHash(),
                appUser.isActive(),   // enabled
                true,                 // accountNonExpired
                true,                 // credentialsNonExpired
                !appUser.isLocked(),  // accountNonLocked
                List.of(new SimpleGrantedAuthority("ROLE_" + appUser.getSystemRole()))
        );
    }
}
