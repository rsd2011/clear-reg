package com.example.batch.ingestion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.DataFeedType;
import com.example.dw.dto.HrEmployeeRecord;
import com.example.dw.dto.HrValidationError;
import com.example.dw.infrastructure.persistence.HrEmployeeStagingRepository;
import com.example.dw.infrastructure.persistence.HrImportErrorRepository;

@ExtendWith(MockitoExtension.class)
class HrStagingServiceTest {

    @Mock
    private HrEmployeeStagingRepository stagingRepository;
    @Mock
    private HrImportErrorRepository errorRepository;

    private HrStagingService service;

    @BeforeEach
    void setUp() {
        service = new HrStagingService(stagingRepository, errorRepository);
    }

    @Test
    void givenRecords_whenPersistRecords_thenSaveEntities() {
        HrEmployeeRecord record = new HrEmployeeRecord("E-1", "Kim", "kim@example.com", "ORG",
                "FULL", "ACTIVE", LocalDate.now(), null, "payload", 2);

        HrImportBatchEntity batch = HrImportBatchEntity.receive(
                "emp.csv", DataFeedType.EMPLOYEE, "SRC", LocalDate.now(), 1, "chk", "/tmp"
        );
        service.persistRecords(batch, List.of(record));

        verify(stagingRepository).save(any());
    }

    @Test
    void givenErrors_whenPersistErrors_thenSaveImportErrors() {
        HrValidationError error = new HrValidationError(1, "E-1", "ERR", "message", "raw");
        HrImportBatchEntity batch = HrImportBatchEntity.receive(
                "emp.csv", DataFeedType.EMPLOYEE, "SRC", LocalDate.now(), 1, "chk", "/tmp"
        );
        service.persistErrors(batch, List.of(error));
        verify(errorRepository).save(any());
    }
}
