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
import com.example.dw.dto.HrOrganizationRecord;
import com.example.dw.dto.HrOrganizationValidationError;
import com.example.dw.infrastructure.persistence.HrImportErrorRepository;
import com.example.dw.infrastructure.persistence.HrOrganizationStagingRepository;

@ExtendWith(MockitoExtension.class)
class HrOrganizationStagingServiceTest {

    @Mock
    private HrOrganizationStagingRepository stagingRepository;
    @Mock
    private HrImportErrorRepository errorRepository;

    private HrOrganizationStagingService service;

    @BeforeEach
    void setUp() {
        service = new HrOrganizationStagingService(stagingRepository, errorRepository);
    }

    @Test
    void givenRecords_whenPersistRecords_thenSaveStagingEntities() {
        HrOrganizationRecord record = new HrOrganizationRecord("ORG", "Org", null, "ACTIVE",
                null, null, LocalDate.now(), null, "payload", 2);
        HrImportBatchEntity batch = HrImportBatchEntity.receive(
                "org.csv", DataFeedType.ORGANIZATION, "SRC", LocalDate.now(), 1, "chk", "/tmp"
        );
        service.persistRecords(batch, List.of(record));
        verify(stagingRepository).save(any());
    }

    @Test
    void givenErrors_whenPersistErrors_thenSaveImportErrors() {
        HrOrganizationValidationError error = new HrOrganizationValidationError(1, "ORG", "ERR", "message", "raw");
        HrImportBatchEntity batch = HrImportBatchEntity.receive(
                "org.csv", DataFeedType.ORGANIZATION, "SRC", LocalDate.now(), 1, "chk", "/tmp"
        );
        service.persistErrors(batch, List.of(error));
        verify(errorRepository).save(any());
    }
}
