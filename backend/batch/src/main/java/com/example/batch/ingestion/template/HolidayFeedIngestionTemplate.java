package com.example.batch.ingestion.template;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import com.example.batch.ingestion.DwHolidayCsvParser;
import com.example.batch.ingestion.DwHolidaySynchronizationService;
import com.example.batch.ingestion.DwHolidayValidationResult;
import com.example.batch.ingestion.DwHolidayValidator;
import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.DataFeedType;
import com.example.dw.dto.DwHolidayRecord;
import com.example.dw.dto.HrSyncResult;

@Component
@RequiredArgsConstructor
public class HolidayFeedIngestionTemplate implements DwFeedIngestionTemplate {

    private final DwHolidayCsvParser parser;
    private final DwHolidayValidator validator;
    private final DwHolidaySynchronizationService synchronizationService;

    @Override
    public DataFeedType supportedType() {
        return DataFeedType.HOLIDAY;
    }

    @Override
    public DwIngestionResult ingest(HrImportBatchEntity batch, String payload) {
        List<DwHolidayRecord> parsed = parser.parse(payload);
        DwHolidayValidationResult validation = validator.validate(parsed);
        HrSyncResult syncResult = synchronizationService.synchronize(batch, validation.validRecords());
        return new DwIngestionResult(parsed.size(), syncResult.insertedRecords(), syncResult.updatedRecords(),
                validation.errors().size());
    }
}
