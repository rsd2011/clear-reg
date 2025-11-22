package com.example.audit.infra.masking;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UnmaskAuditRepository extends JpaRepository<UnmaskAuditRecord, UUID> {
}
