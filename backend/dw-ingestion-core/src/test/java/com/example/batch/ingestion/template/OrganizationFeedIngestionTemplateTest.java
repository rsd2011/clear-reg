package com.example.batch.ingestion.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.batch.ingestion.HrOrganizationCsvParser;
import com.example.batch.ingestion.HrOrganizationStagingService;
import com.example.batch.ingestion.HrOrganizationSynchronizationService;
import com.example.batch.ingestion.HrOrganizationValidator;
import com.example.dw.application.DwOrganizationTreeService;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.HrOrganizationRecord;
import com.example.dw.dto.HrOrganizationValidationError;
import com.example.dw.dto.HrOrganizationValidationResult;
import com.example.dw.dto.HrSyncResult;

class OrganizationFeedIngestionTemplateTest {

    HrOrganizationCsvParser parser = Mockito.mock(HrOrganizationCsvParser.class);
    HrOrganizationValidator validator = Mockito.mock(HrOrganizationValidator.class);
    HrOrganizationStagingService stagingService = Mockito.mock(HrOrganizationStagingService.class);
    HrOrganizationSynchronizationService syncService = Mockito.mock(HrOrganizationSynchronizationService.class);
    DwOrganizationTreeService treeService = Mockito.mock(DwOrganizationTreeService.class);

    OrganizationFeedIngestionTemplate template = new OrganizationFeedIngestionTemplate(parser, validator, stagingService, syncService, treeService);

    @Test
    @DisplayName("조직 피드를 파싱·검증·동기화하고 트리를 무효화한다")
    void ingestHappyPath() {
        HrImportBatchEntity batch = new HrImportBatchEntity();
        HrOrganizationRecord record = new HrOrganizationRecord("ORG1", "이름", "PARENT", "ACTIVE",
                LocalDate.of(2024,1,1), null, "raw", 2);
        List<HrOrganizationRecord> parsed = List.of(record);
        when(parser.parse("csv")).thenReturn(parsed);
        HrOrganizationValidationResult validation = new HrOrganizationValidationResult(parsed, List.of());
        when(validator.validate(parsed)).thenReturn(validation);
        when(syncService.synchronize(batch, parsed)).thenReturn(new HrSyncResult(1,0));

        DwIngestionResult result = template.ingest(batch, "csv");

        verify(stagingService).persistRecords(batch, parsed);
        verify(stagingService).persistErrors(batch, List.of());
        verify(treeService).evict();
        assertThat(result.totalRecords()).isEqualTo(1);
        assertThat(result.failedRecords()).isZero();
    }
}

