package com.example.batch.ingestion.template;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import com.example.batch.ingestion.HrOrganizationCsvParser;
import com.example.batch.ingestion.HrOrganizationStagingService;
import com.example.batch.ingestion.HrOrganizationSynchronizationService;
import com.example.batch.ingestion.HrOrganizationValidator;
import com.example.dw.application.DwOrganizationTreeService;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.DataFeedType;
import com.example.dw.dto.HrOrganizationRecord;
import com.example.dw.dto.HrOrganizationValidationResult;
import com.example.dw.dto.HrSyncResult;

@Component
@RequiredArgsConstructor
public class OrganizationFeedIngestionTemplate implements DwFeedIngestionTemplate {

    private final HrOrganizationCsvParser parser;
    private final HrOrganizationValidator validator;
    private final HrOrganizationStagingService stagingService;
    private final HrOrganizationSynchronizationService synchronizationService;
    private final DwOrganizationTreeService organizationTreeService;

    @Override
    public DataFeedType supportedType() {
        return DataFeedType.ORGANIZATION;
    }

    @Override
    public DwIngestionResult ingest(HrImportBatchEntity batch, String payload) {
        List<HrOrganizationRecord> parsed = parser.parse(payload);
        HrOrganizationValidationResult validation = validator.validate(parsed);
        stagingService.persistRecords(batch, validation.validRecords());
        stagingService.persistErrors(batch, validation.errors());
        HrSyncResult syncResult = synchronizationService.synchronize(batch, validation.validRecords());
        organizationTreeService.evict();
        return new DwIngestionResult(parsed.size(), syncResult.insertedRecords(), syncResult.updatedRecords(),
                validation.errors().size());
    }
}
