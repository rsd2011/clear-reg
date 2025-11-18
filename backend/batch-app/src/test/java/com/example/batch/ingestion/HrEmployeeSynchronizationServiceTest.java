package com.example.batch.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.hr.domain.HrEmployeeEntity;
import com.example.hr.domain.HrImportBatchEntity;
import com.example.hr.dto.HrEmployeeRecord;
import com.example.hr.dto.HrSyncResult;
import com.example.hr.infrastructure.persistence.HrEmployeeRepository;

@ExtendWith(MockitoExtension.class)
class HrEmployeeSynchronizationServiceTest {

    @Mock
    private HrEmployeeRepository repository;

    private HrEmployeeSynchronizationService service;

    @BeforeEach
    void setUp() {
        service = new HrEmployeeSynchronizationService(repository);
    }

    @Test
    void givenNewRecord_whenSynchronize_thenInsertSnapshot() {
        HrEmployeeRecord record = new HrEmployeeRecord("E-1", "Kim", "kim@example.com", "ORG",
                "FULL", "ACTIVE", LocalDate.now(), null, "payload", 2);
        given(repository.findActive("E-1")).willReturn(Optional.empty());

        HrSyncResult result = service.synchronize(new HrImportBatchEntity(), List.of(record));

        assertThat(result.insertedRecords()).isEqualTo(1);
        verify(repository).save(any(HrEmployeeEntity.class));
    }

    @Test
    void givenExistingRecord_whenStateChanges_thenUpdateAndInsert() {
        HrEmployeeEntity active = new HrEmployeeEntity();
        active.setEmployeeId("E-1");
        active.setVersion(1);
        active.setEffectiveStart(LocalDate.now().minusDays(10));
        HrEmployeeRecord record = new HrEmployeeRecord("E-1", "Kim", "kim@example.com", "ORG",
                "FULL", "ACTIVE", LocalDate.now(), null, "payload", 2);
        given(repository.findActive("E-1")).willReturn(Optional.of(active));

        HrSyncResult result = service.synchronize(new HrImportBatchEntity(), List.of(record));

        assertThat(result.updatedRecords()).isEqualTo(1);
        verify(repository).save(active);
    }
}
