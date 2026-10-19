package com.mycropdiary.api.repository;

import com.mycropdiary.api.entity.AccountToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountTokenRepository extends JpaRepository<AccountToken, Integer> {

    Optional<AccountToken> findFirstByUser_IdAndTokenTypeAndUsedAtIsNullOrderByCreatedAtDesc(
            Integer userId, String tokenType);

    List<AccountToken> findAllByUser_IdAndTokenTypeAndUsedAtIsNull(
            Integer userId, String tokenType);

    Optional<AccountToken> findByTokenHashAndTokenType(String tokenHash, String tokenType);
}
