package com.example.hr.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.example.hr.domain.HrImportBatchEntity;
import com.example.hr.domain.repository.HrBatchRepository;

@Repository
public class JpaHrBatchRepository implements HrBatchRepository {

    private final SpringDataHrImportBatchRepository delegate;

    public JpaHrBatchRepository(SpringDataHrImportBatchRepository delegate) {
        this.delegate = delegate;
    }

    @Override
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
