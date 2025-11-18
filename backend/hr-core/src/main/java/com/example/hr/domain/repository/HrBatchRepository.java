package com.example.hr.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.hr.domain.HrImportBatchEntity;

public interface HrBatchRepository {

    HrImportBatchEntity save(HrImportBatchEntity entity);

    Optional<HrImportBatchEntity> findLatest();

    Page<HrImportBatchEntity> findAll(Pageable pageable);
}
