package com.example.dw.application;

import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPort;
import com.example.audit.RiskLevel;
import com.example.audit.Subject;
import com.example.common.cache.CacheNames;
import com.example.dw.infrastructure.persistence.HrEmployeeRepository;

@Service
@RequiredArgsConstructor
public class DwEmployeeDirectoryService {

    private final HrEmployeeRepository employeeRepository;
    private final AuditPort auditPort;

    @Cacheable(cacheNames = CacheNames.DW_EMPLOYEES, key = "#employeeId", unless = "#result.isEmpty()")
    public Optional<DwEmployeeSnapshot> findActive(String employeeId) {
        Optional<DwEmployeeSnapshot> result = employeeRepository.findActive(employeeId).map(DwEmployeeSnapshot::fromEntity);
        auditLookup(employeeId, result.isPresent());
        return result;
    }

    @CacheEvict(cacheNames = CacheNames.DW_EMPLOYEES, key = "#employeeId")
    public void evict(String employeeId) {
        // eviction only
    }

    @CacheEvict(cacheNames = CacheNames.DW_EMPLOYEES, allEntries = true)
    public void evictAll() {
        // eviction only
    }

    private void auditLookup(String employeeId, boolean success) {
        AuditEvent event = AuditEvent.builder()
                .eventType("DW_EMPLOYEE_LOOKUP")
                .moduleName("data-integration")
                .action("DW_EMPLOYEE_FIND_ACTIVE")
                .actor(Actor.builder().id("dw-ingestion").type(ActorType.SYSTEM).build())
                .subject(Subject.builder().type("EMPLOYEE").key(employeeId).build())
                .success(success)
                .resultCode(success ? "OK" : "NOT_FOUND")
                .riskLevel(RiskLevel.LOW)
                .build();
        try {
            auditPort.record(event, AuditMode.ASYNC_FALLBACK);
        } catch (Exception ignore) {
            // 감사 실패가 업무 흐름을 막지 않도록 삼킴
        }
    }
}
