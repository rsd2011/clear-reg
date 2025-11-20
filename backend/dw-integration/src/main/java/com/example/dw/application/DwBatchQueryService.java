package com.example.dw.application;

import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.domain.repository.HrBatchRepository;
import com.example.common.cache.CacheNames;

@Service
@RequiredArgsConstructor
public class DwBatchQueryService {

    private final HrBatchRepository batchRepository;

    public Page<HrImportBatchEntity> getBatches(Pageable pageable) {
        return batchRepository.findAll(pageable);
    }

    @Cacheable(cacheNames = CacheNames.LATEST_DW_BATCH, key = "'singleton'", sync = true)
    public Optional<HrImportBatchEntity> latestBatch() {
        return batchRepository.findLatest();
    }
}
