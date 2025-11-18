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

import com.example.hr.domain.HrImportBatchEntity;
import com.example.hr.dto.HrEmployeeRecord;
import com.example.hr.dto.HrValidationError;
import com.example.hr.infrastructure.persistence.HrEmployeeStagingRepository;
import com.example.hr.infrastructure.persistence.HrImportErrorRepository;

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

        service.persistRecords(new HrImportBatchEntity(), List.of(record));

        verify(stagingRepository).save(any());
    }

    @Test
    void givenErrors_whenPersistErrors_thenSaveImportErrors() {
        HrValidationError error = new HrValidationError(1, "E-1", "ERR", "message", "raw");
        service.persistErrors(new HrImportBatchEntity(), List.of(error));
        verify(errorRepository).save(any());
    }
}
