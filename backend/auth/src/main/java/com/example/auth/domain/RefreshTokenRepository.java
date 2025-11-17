package com.example.auth.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteByUser(UserAccount userAccount);

    List<RefreshToken> findByUserOrderByCreatedAtAsc(UserAccount userAccount);
}
