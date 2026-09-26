package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.AccountToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

// [AI_CHANGE] Root cause: Cần truy vấn AccountToken theo user + type + trạng thái để xác thực OTP và refresh token
public interface AccountTokenRepository extends JpaRepository<AccountToken, Long> {

    Optional<AccountToken> findFirstByUser_IdAndTokenTypeAndUsedAtIsNullAndRevokedAtIsNullOrderByCreatedAtDesc(
            Long userId, String tokenType);

    List<AccountToken> findAllByUser_IdAndTokenTypeAndUsedAtIsNullAndRevokedAtIsNull(
            Long userId, String tokenType);

    Optional<AccountToken> findByTokenHashAndTokenType(String tokenHash, String tokenType);
}
