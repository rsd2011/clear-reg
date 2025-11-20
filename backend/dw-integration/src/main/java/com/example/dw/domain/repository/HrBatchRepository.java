package com.example.dw.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.dw.domain.HrImportBatchEntity;

public interface HrBatchRepository {

    HrImportBatchEntity save(HrImportBatchEntity entity);

    Optional<HrImportBatchEntity> findLatest();

    Page<HrImportBatchEntity> findAll(Pageable pageable);
}
