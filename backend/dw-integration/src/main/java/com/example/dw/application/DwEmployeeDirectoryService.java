package com.example.dw.application;

import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.example.common.cache.CacheNames;
import com.example.dw.infrastructure.persistence.HrEmployeeRepository;

@Service
@RequiredArgsConstructor
public class DwEmployeeDirectoryService {

    private final HrEmployeeRepository employeeRepository;

    @Cacheable(cacheNames = CacheNames.DW_EMPLOYEES, key = "#employeeId", unless = "#result.isEmpty()")
    public Optional<DwEmployeeSnapshot> findActive(String employeeId) {
        return employeeRepository.findActive(employeeId).map(DwEmployeeSnapshot::fromEntity);
    }

    @CacheEvict(cacheNames = CacheNames.DW_EMPLOYEES, key = "#employeeId")
    public void evict(String employeeId) {
        // eviction only
    }

    @CacheEvict(cacheNames = CacheNames.DW_EMPLOYEES, allEntries = true)
    public void evictAll() {
        // eviction only
    }
}
