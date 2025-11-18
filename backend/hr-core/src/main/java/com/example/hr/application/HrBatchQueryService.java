package com.example.hr.application;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.example.hr.domain.HrImportBatchEntity;
import com.example.hr.domain.repository.HrBatchRepository;

@Service
@RequiredArgsConstructor
public class HrBatchQueryService {

    private final HrBatchRepository batchRepository;

    public Page<HrImportBatchEntity> getBatches(Pageable pageable) {
        return batchRepository.findAll(pageable);
    }

    public Optional<HrImportBatchEntity> latestBatch() {
        return batchRepository.findLatest();
    }
}
