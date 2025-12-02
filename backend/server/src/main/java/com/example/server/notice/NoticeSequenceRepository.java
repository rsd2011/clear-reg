package com.example.server.notice;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface NoticeSequenceRepository extends JpaRepository<NoticeSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<NoticeSequence> findBySequenceYear(Integer sequenceYear);
}
