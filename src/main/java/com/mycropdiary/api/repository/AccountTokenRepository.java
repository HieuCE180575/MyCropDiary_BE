package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.AccountToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountTokenRepository extends JpaRepository<AccountToken, UUID> {

    Optional<AccountToken> findFirstByUser_IdAndTokenTypeAndUsedAtIsNullOrderByCreatedAtDesc(
            UUID userId, String tokenType);

    List<AccountToken> findAllByUser_IdAndTokenTypeAndUsedAtIsNull(
            UUID userId, String tokenType);

    Optional<AccountToken> findByTokenHashAndTokenType(String tokenHash, String tokenType);
}
