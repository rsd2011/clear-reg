package com.example.dw.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.audit.AuditPort;
import com.example.dw.domain.HrEmployeeEntity;
import com.example.dw.infrastructure.persistence.HrEmployeeRepository;

@ExtendWith(MockitoExtension.class)
class DwEmployeeDirectoryServiceTest {

    @Mock
    private HrEmployeeRepository employeeRepository;
    @Mock
    private AuditPort auditPort;

    private DwEmployeeDirectoryService service;

    @BeforeEach
    void setUp() {
        service = new DwEmployeeDirectoryService(employeeRepository, auditPort);
    }

    @Test
    void givenActiveEmployee_whenFind_thenReturnSnapshot() {
        HrEmployeeEntity entity = employee("EMP-1");
        given(employeeRepository.findActive("EMP-1")).willReturn(Optional.of(entity));

        Optional<DwEmployeeSnapshot> snapshot = service.findActive("EMP-1");

        assertThat(snapshot).isPresent();
        assertThat(snapshot.orElseThrow().fullName()).isEqualTo("Employee-EMP-1");
        verify(employeeRepository).findActive("EMP-1");
    }

    @Test
    void givenMissingEmployee_whenFind_thenReturnEmptyOptional() {
        given(employeeRepository.findActive("EMP-2")).willReturn(Optional.empty());

        Optional<DwEmployeeSnapshot> snapshot = service.findActive("EMP-2");

        assertThat(snapshot).isEmpty();
    }

    @Test
    void whenEvictMethodsInvoked_thenNoException() {
        service.evict("EMP-1");
        service.evictAll();
    }

    private HrEmployeeEntity employee(String id) {
        return HrEmployeeEntity.snapshot(
                id,
                3,
                "Employee-" + id,
                id.toLowerCase() + "@corp.dev",
                "ROOT",
                "FULL_TIME",
                "ACTIVE",
                LocalDate.of(2020, 1, 1),
                null,
                UUID.randomUUID(),
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }
}
