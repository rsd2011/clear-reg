package com.example.auth.domain;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, UUID> {

    List<PasswordHistory> findTopByUserOrderByChangedAtDesc(UserAccount user);

    List<PasswordHistory> findByUserOrderByChangedAtDesc(UserAccount user);
}
