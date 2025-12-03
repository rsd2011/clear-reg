package com.example.batch.ingestion.template;

import com.example.dw.domain.HrImportBatchEntity;
import com.example.dw.dto.DataFeedType;

public interface DwFeedIngestionTemplate {

    DataFeedType supportedType();

    DwIngestionResult ingest(HrImportBatchEntity batch, String payload);
}
