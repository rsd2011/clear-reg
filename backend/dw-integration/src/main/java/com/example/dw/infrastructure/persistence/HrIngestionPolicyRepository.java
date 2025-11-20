package com.example.dw.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.dw.domain.HrIngestionPolicyEntity;

public interface HrIngestionPolicyRepository extends JpaRepository<HrIngestionPolicyEntity, UUID> {

    Optional<HrIngestionPolicyEntity> findByCode(String code);
}
