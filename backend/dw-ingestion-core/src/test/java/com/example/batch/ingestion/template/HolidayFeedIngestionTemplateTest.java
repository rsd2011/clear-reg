package com.example.batch.ingestion.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.batch.ingestion.DwHolidayCsvParser;
import com.example.batch.ingestion.DwHolidaySynchronizationService;
import com.example.batch.ingestion.DwHolidayValidationResult;
import com.example.batch.ingestion.DwHolidayValidator;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.DwHolidayRecord;
import com.example.dw.dto.HrSyncResult;

class HolidayFeedIngestionTemplateTest {

    DwHolidayCsvParser parser = Mockito.mock(DwHolidayCsvParser.class);
    DwHolidayValidator validator = Mockito.mock(DwHolidayValidator.class);
    DwHolidaySynchronizationService syncService = Mockito.mock(DwHolidaySynchronizationService.class);

    HolidayFeedIngestionTemplate template = new HolidayFeedIngestionTemplate(parser, validator, syncService);

    @Test
    @DisplayName("휴일 피드를 파싱·검증·동기화한다")
    void ingestHappyPath() {
        HrImportBatchEntity batch = new HrImportBatchEntity();
        List<DwHolidayRecord> parsed = List.of(new DwHolidayRecord(LocalDate.of(2025,1,1), "KR", "신정", "New Year", false));
        when(parser.parse("csv")).thenReturn(parsed);
        DwHolidayValidationResult validation = new DwHolidayValidationResult(parsed, List.of());
        when(validator.validate(parsed)).thenReturn(validation);
        when(syncService.synchronize(batch, parsed)).thenReturn(new HrSyncResult(1,0));

        DwIngestionResult result = template.ingest(batch, "csv");

        verify(syncService).synchronize(batch, parsed);
        assertThat(result.totalRecords()).isEqualTo(1);
        assertThat(result.failedRecords()).isZero();
    }
}

