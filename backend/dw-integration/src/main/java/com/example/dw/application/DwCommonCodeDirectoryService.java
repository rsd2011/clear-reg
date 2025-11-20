package com.example.dw.application;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import lombok.RequiredArgsConstructor;

import com.example.common.cache.CacheNames;
import com.example.dw.infrastructure.persistence.DwCommonCodeRepository;

@Service
@RequiredArgsConstructor
public class DwCommonCodeDirectoryService {

    private final DwCommonCodeRepository repository;

    @Cacheable(cacheNames = CacheNames.DW_COMMON_CODES, key = "#codeType.toUpperCase()", unless = "#result.isEmpty()")
    public List<DwCommonCodeSnapshot> findActive(String codeType) {
        Assert.hasText(codeType, "codeType must not be blank");
        String normalized = codeType.toUpperCase(java.util.Locale.ROOT);
        return repository.findByCodeTypeAndActiveTrueOrderByDisplayOrderAscCodeValueAsc(normalized).stream()
                .map(DwCommonCodeSnapshot::fromEntity)
                .toList();
    }

    @CacheEvict(cacheNames = CacheNames.DW_COMMON_CODES, key = "#codeType.toUpperCase()")
    public void evict(String codeType) {
        // cache eviction only
    }

    @CacheEvict(cacheNames = CacheNames.DW_COMMON_CODES, allEntries = true)
    public void evictAll() {
        // cache eviction only
    }
}
