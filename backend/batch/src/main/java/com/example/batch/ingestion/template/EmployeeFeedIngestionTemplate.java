package com.example.batch.ingestion.template;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import com.example.batch.ingestion.HrEmployeeSynchronizationService;
import com.example.batch.ingestion.HrRecordValidator;
import com.example.batch.ingestion.HrStagingService;
import com.example.batch.ingestion.HrCsvRecordParser;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.DataFeedType;
import com.example.dw.dto.HrEmployeeRecord;
import com.example.dw.dto.HrSyncResult;
import com.example.dw.dto.HrValidationResult;

@Component
@RequiredArgsConstructor
public class EmployeeFeedIngestionTemplate implements DwFeedIngestionTemplate {

    private final HrCsvRecordParser parser;
    private final HrRecordValidator validator;
    private final HrStagingService stagingService;
    private final HrEmployeeSynchronizationService synchronizationService;

    @Override
    public DataFeedType supportedType() {
        return DataFeedType.EMPLOYEE;
    }

    @Override
    public DwIngestionResult ingest(HrImportBatchEntity batch, String payload) {
        List<HrEmployeeRecord> parsed = parser.parse(payload);
        HrValidationResult validation = validator.validate(parsed);
        stagingService.persistRecords(batch, validation.validRecords());
        stagingService.persistErrors(batch, validation.errors());
        HrSyncResult syncResult = synchronizationService.synchronize(batch, validation.validRecords());
        return new DwIngestionResult(parsed.size(), syncResult.insertedRecords(), syncResult.updatedRecords(),
                validation.errors().size());
    }
}
