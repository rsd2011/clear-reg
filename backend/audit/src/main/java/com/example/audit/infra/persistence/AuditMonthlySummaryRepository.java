package com.example.audit.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditMonthlySummaryRepository extends JpaRepository<AuditMonthlySummaryEntity, String> {
}
