package com.example.batch.ingestion.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.batch.ingestion.HrEmployeeSynchronizationService;
import com.example.batch.ingestion.HrRecordValidator;
import com.example.batch.ingestion.HrStagingService;
import com.example.batch.ingestion.HrCsvRecordParser;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.HrEmployeeRecord;
import com.example.dw.dto.HrSyncResult;
import com.example.dw.dto.HrValidationResult;
import com.example.dw.dto.HrValidationError;

class EmployeeFeedIngestionTemplateTest {

    HrCsvRecordParser parser = Mockito.mock(HrCsvRecordParser.class);
    HrRecordValidator validator = Mockito.mock(HrRecordValidator.class);
    HrStagingService stagingService = Mockito.mock(HrStagingService.class);
    HrEmployeeSynchronizationService syncService = Mockito.mock(HrEmployeeSynchronizationService.class);

    EmployeeFeedIngestionTemplate template = new EmployeeFeedIngestionTemplate(parser, validator, stagingService, syncService);

    @Test
    @DisplayName("직원 피드를 파싱·검증·동기화하고 결과를 반환한다")
    void ingestHappyPath() {
        HrImportBatchEntity batch = new HrImportBatchEntity();
        HrEmployeeRecord record = new HrEmployeeRecord(
                "EMP-1", "홍길동", "hong@example.com", "ORG1", "FULLTIME", "ACTIVE",
                LocalDate.of(2025, 1, 1), null, "raw", 2);
        List<HrEmployeeRecord> parsed = List.of(record);
        when(parser.parse("csv"))
                .thenReturn(parsed);
        HrValidationResult validation = new HrValidationResult(parsed, List.of());
        when(validator.validate(parsed)).thenReturn(validation);
        when(syncService.synchronize(batch, parsed)).thenReturn(new HrSyncResult(1, 0));

        DwIngestionResult result = template.ingest(batch, "csv");

        verify(stagingService).persistRecords(batch, parsed);
        verify(stagingService).persistErrors(batch, List.of());
        assertThat(result.totalRecords()).isEqualTo(1);
        assertThat(result.failedRecords()).isZero();
    }
}

