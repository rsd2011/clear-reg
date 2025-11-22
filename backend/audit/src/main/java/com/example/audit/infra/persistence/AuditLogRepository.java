package com.example.audit.infra.persistence;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    Optional<AuditLogEntity> findTopByOrderByEventTimeDesc();
}
