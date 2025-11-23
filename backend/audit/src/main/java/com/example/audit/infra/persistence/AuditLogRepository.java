package com.example.audit.infra.persistence;

import java.util.UUID;
import java.util.Optional;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    Optional<AuditLogEntity> findTopByOrderByEventTimeDesc();

    long deleteByEventTimeBefore(Instant threshold);

    long countByEventTimeBetween(Instant startInclusive, Instant endExclusive);

    long countByEventTimeBetweenAndSuccess(Instant startInclusive, Instant endExclusive, boolean success);

    long countByEventTimeBetweenAndEventTypeIn(Instant startInclusive, Instant endExclusive, Iterable<String> eventTypes);
}
