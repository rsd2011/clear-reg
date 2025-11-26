package com.example.batch.ingestion.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.batch.ingestion.DwCommonCodeCsvParser;
import com.example.batch.ingestion.DwCommonCodeSynchronizationService;
import com.example.batch.ingestion.DwCommonCodeValidationResult;
import com.example.batch.ingestion.DwCommonCodeValidator;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.DataFeedType;
import com.example.dw.dto.DwCommonCodeRecord;
import com.example.dw.dto.HrSyncResult;

@ExtendWith(MockitoExtension.class)
class CommonCodeFeedIngestionTemplateTest {

    @Mock
    private DwCommonCodeCsvParser parser;
    @Mock
    private DwCommonCodeValidator validator;
    @Mock
    private DwCommonCodeSynchronizationService synchronizationService;

    private CommonCodeFeedIngestionTemplate template;

    @BeforeEach
    void setUp() {
        template = new CommonCodeFeedIngestionTemplate(parser, validator, synchronizationService);
    }

    @Test
    void givenPayload_whenIngest_thenReturnDwIngestionResult() {
        given(parser.parse("payload")).willReturn(List.of(new DwCommonCodeRecord("TYPE", "V", "Name", 1, true, null, null, null, 2)));
        given(validator.validate(any())).willReturn(new DwCommonCodeValidationResult(List.of(), List.of()));
        given(synchronizationService.synchronize(any(), any())).willReturn(new HrSyncResult(0, 0));

        HrImportBatchEntity batch = HrImportBatchEntity.receive(
                "common.csv", DataFeedType.COMMON_CODE, "SRC", LocalDate.now(), 1, "chk", "/tmp"
        );
        DwIngestionResult result = template.ingest(batch, "payload");

        assertThat(template.supportedType()).isEqualTo(DataFeedType.COMMON_CODE);
        assertThat(result.totalRecords()).isEqualTo(1);
    }
}
