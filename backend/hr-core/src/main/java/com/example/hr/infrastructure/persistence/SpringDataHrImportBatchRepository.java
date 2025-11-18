package com.example.hr.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.hr.domain.HrImportBatchEntity;

public interface SpringDataHrImportBatchRepository extends JpaRepository<HrImportBatchEntity, UUID> {

    Optional<HrImportBatchEntity> findFirstByOrderByReceivedAtDesc();

    Page<HrImportBatchEntity> findAllByOrderByReceivedAtDesc(Pageable pageable);
}
