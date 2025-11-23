package com.example.dw.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.audit.AuditPort;
import com.example.dw.domain.HrEmployeeEntity;
import com.example.dw.infrastructure.persistence.HrEmployeeRepository;

class DwEmployeeDirectoryServiceAuditTest {

    HrEmployeeRepository repo = Mockito.mock(HrEmployeeRepository.class);
    AuditPort auditPort = Mockito.mock(AuditPort.class);
    DwEmployeeDirectoryService service = new DwEmployeeDirectoryService(repo, auditPort);

    private HrEmployeeEntity employeeEntity() {
        HrEmployeeEntity e = new HrEmployeeEntity();
        e.setEmployeeId("E1");
        e.setVersion(1);
        e.setFullName("홍길동");
        e.setEmail("a@b.com");
        e.setOrganizationCode("ORG1");
        e.setEmploymentType("FULL");
        e.setEmploymentStatus("ACTIVE");
        e.setEffectiveStart(LocalDate.of(2024, 1, 1));
        e.setEffectiveEnd(LocalDate.of(2024, 12, 31));
        e.setSourceBatchId(UUID.fromString("123e4567-e89b-12d3-a456-426614174001"));
        e.setSyncedAt(OffsetDateTime.of(2024, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC));
        return e;
    }

    @Test
    @DisplayName("직원 조회 성공 시 Audit resultCode=OK로 기록한다")
    void auditSuccess() {
        when(repo.findActive("E1")).thenReturn(Optional.of(employeeEntity()));

        service.findActive("E1");

        verify(auditPort).record(any(), any());
    }

    @Test
    @DisplayName("AuditPort가 예외를 던져도 흐름을 막지 않는다")
    void auditFailureIsSwallowed() {
        when(repo.findActive("E2")).thenReturn(Optional.empty());
        doThrow(new RuntimeException("audit down")).when(auditPort).record(any(), any());

        Optional<DwEmployeeSnapshot> result = service.findActive("E2");

        assertThat(result).isEmpty();
    }
}
