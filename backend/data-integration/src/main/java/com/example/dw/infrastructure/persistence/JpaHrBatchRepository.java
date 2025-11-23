package com.example.dw.infrastructure.persistence;

import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.domain.repository.HrBatchRepository;
import com.example.common.cache.CacheNames;

@Repository
public class JpaHrBatchRepository implements HrBatchRepository {

    private final SpringDataHrImportBatchRepository delegate;

    public JpaHrBatchRepository(SpringDataHrImportBatchRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.LATEST_DW_BATCH, allEntries = true)
    public HrImportBatchEntity save(HrImportBatchEntity entity) {
        return delegate.save(entity);
    }

    @Override
    public Optional<HrImportBatchEntity> findLatest() {
        return delegate.findFirstByOrderByReceivedAtDesc();
    }

    @Override
    public Page<HrImportBatchEntity> findAll(Pageable pageable) {
        return delegate.findAllByOrderByReceivedAtDesc(pageable);
    }
}
