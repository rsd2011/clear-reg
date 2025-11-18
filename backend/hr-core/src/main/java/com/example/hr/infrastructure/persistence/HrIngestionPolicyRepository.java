package com.example.hr.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.hr.domain.HrIngestionPolicyEntity;

public interface HrIngestionPolicyRepository extends JpaRepository<HrIngestionPolicyEntity, UUID> {

    Optional<HrIngestionPolicyEntity> findByCode(String code);
}
