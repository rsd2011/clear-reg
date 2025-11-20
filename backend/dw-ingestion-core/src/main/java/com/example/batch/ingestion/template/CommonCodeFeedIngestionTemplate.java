package com.example.batch.ingestion.template;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import com.example.batch.ingestion.DwCommonCodeCsvParser;
import com.example.batch.ingestion.DwCommonCodeSynchronizationService;
import com.example.batch.ingestion.DwCommonCodeValidationResult;
import com.example.batch.ingestion.DwCommonCodeValidator;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.DataFeedType;
import com.example.dw.dto.DwCommonCodeRecord;
import com.example.dw.dto.HrSyncResult;

@Component
@RequiredArgsConstructor
public class CommonCodeFeedIngestionTemplate implements DwFeedIngestionTemplate {

    private final DwCommonCodeCsvParser parser;
    private final DwCommonCodeValidator validator;
    private final DwCommonCodeSynchronizationService synchronizationService;

    @Override
    public DataFeedType supportedType() {
        return DataFeedType.COMMON_CODE;
    }

    @Override
    public DwIngestionResult ingest(HrImportBatchEntity batch, String payload) {
        List<DwCommonCodeRecord> parsed = parser.parse(payload);
        DwCommonCodeValidationResult validation = validator.validate(parsed);
        HrSyncResult syncResult = synchronizationService.synchronize(batch, validation.validRecords());
        return new DwIngestionResult(parsed.size(), syncResult.insertedRecords(), syncResult.updatedRecords(),
                validation.errors().size());
    }
}
