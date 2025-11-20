package com.example.batch.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.batch.repeat.RepeatStatus;

import com.example.batch.ingestion.DwIngestionService;
import com.example.dw.application.job.DwIngestionJob;
import com.example.dw.application.job.DwIngestionOutboxService;

class DwBatchConfigurationTest {

    @Test
    void givenTasklet_whenExecuted_thenDelegatesToService() throws Exception {
        DwIngestionService ingestionService = mock(DwIngestionService.class);
        DwIngestionOutboxService outboxService = mock(DwIngestionOutboxService.class);
        DwBatchConfiguration configuration = new DwBatchConfiguration(ingestionService, outboxService);

        RepeatStatus status = configuration.dwIngestionTasklet().execute(null, null);

        assertThat(status).isEqualTo(RepeatStatus.FINISHED);
        verify(outboxService).enqueue(DwIngestionJob.fetchNext());
    }
}
