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

import com.example.dw.application.DwEmployeeDirectoryService;
import com.example.dw.domain.HrEmployeeEntity;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.DataFeedType;
import com.example.dw.dto.HrEmployeeRecord;
import com.example.dw.dto.HrSyncResult;
import com.example.dw.infrastructure.persistence.HrEmployeeRepository;

@ExtendWith(MockitoExtension.class)
class HrEmployeeSynchronizationServiceTest {

    @Mock
    private HrEmployeeRepository repository;
    @Mock
    private DwEmployeeDirectoryService directoryService;

    private HrEmployeeSynchronizationService service;

    @BeforeEach
    void setUp() {
        service = new HrEmployeeSynchronizationService(repository, directoryService);
    }

    @Test
    void givenNewRecord_whenSynchronize_thenInsertSnapshot() {
        HrEmployeeRecord record = new HrEmployeeRecord("E-1", "Kim", "kim@example.com", "ORG",
                "FULL", "ACTIVE", LocalDate.now(), null, "payload", 2);
        given(repository.findActive("E-1")).willReturn(Optional.empty());

        HrImportBatchEntity batch = HrImportBatchEntity.receive(
                "emp.csv", DataFeedType.EMPLOYEE, "SRC", LocalDate.now(), 1, "chk", "/tmp"
        );
        HrSyncResult result = service.synchronize(batch, List.of(record));

        assertThat(result.insertedRecords()).isEqualTo(1);
        verify(repository).save(any(HrEmployeeEntity.class));
        verify(directoryService).evict("E-1");
    }

    @Test
    void givenExistingRecord_whenStateChanges_thenUpdateAndInsert() {
        HrEmployeeEntity active = HrEmployeeEntity.snapshot(
                "E-1",
                1,
                "Old",
                "old@example.com",
                "ORG",
                "FULL",
                "ACTIVE",
                LocalDate.now().minusDays(10),
                null,
                java.util.UUID.randomUUID(),
                java.time.OffsetDateTime.now()
        );
        HrEmployeeRecord record = new HrEmployeeRecord("E-1", "Kim", "kim@example.com", "ORG",
                "FULL", "ACTIVE", LocalDate.now(), null, "payload", 2);
        given(repository.findActive("E-1")).willReturn(Optional.of(active));

        HrSyncResult result = service.synchronize(new HrImportBatchEntity(), List.of(record));

        assertThat(result.updatedRecords()).isEqualTo(1);
        verify(repository).save(active);
        verify(directoryService).evict("E-1");
    }
}
