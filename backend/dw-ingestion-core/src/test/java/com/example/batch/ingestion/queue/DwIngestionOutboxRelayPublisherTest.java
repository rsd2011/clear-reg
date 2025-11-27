package com.example.batch.ingestion.queue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import com.example.common.policy.PolicySettingsProvider;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.dw.application.job.DwIngestionJobQueue;
import com.example.dw.application.job.DwIngestionOutboxService;
import com.example.dw.application.job.DwIngestionOutboxStatus;
import com.example.dw.application.job.OutboxMessagePublisher;
import com.example.dw.domain.DwIngestionOutbox;

@SuppressWarnings("unchecked")
class DwIngestionOutboxRelayPublisherTest {

    @Test
    void relayPublishesOutboxEntry() {
        DwIngestionOutboxService outboxService = Mockito.mock(DwIngestionOutboxService.class);
        DwIngestionJobQueue jobQueue = Mockito.mock(DwIngestionJobQueue.class);
        OutboxMessagePublisher publisher = Mockito.mock(OutboxMessagePublisher.class);
        DwIngestionOutboxRelay relay = newRelay(outboxService, jobQueue, publisher, 10);

        DwIngestionOutbox entry = new DwIngestionOutbox(com.example.dw.application.job.DwIngestionJobType.FETCH_NEXT,
                java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now());
        entry.markSending(java.time.Clock.systemUTC(), "test");
        when(outboxService.claimPending(10)).thenReturn(List.of(entry));

        relay.relay();

        verify(jobQueue).enqueue(any());
        verify(publisher, times(1)).publish(entry);
        org.assertj.core.api.Assertions.assertThat(relay.trigger()).isNotNull();
    }

    @Test
    void relayDoesNothingWhenNoEntries() {
        DwIngestionOutboxService outboxService = Mockito.mock(DwIngestionOutboxService.class);
        DwIngestionJobQueue jobQueue = Mockito.mock(DwIngestionJobQueue.class);
        OutboxMessagePublisher publisher = Mockito.mock(OutboxMessagePublisher.class);
        DwIngestionOutboxRelay relay = newRelay(outboxService, jobQueue, publisher, 5);

        org.assertj.core.api.Assertions.assertThatNoException().isThrownBy(relay::relay);
        Mockito.verify(outboxService).claimPending(5);
        Mockito.verifyNoInteractions(jobQueue, publisher);
    }

    @Test
    void relayMarksFailedWhenPublisherThrows() {
        DwIngestionOutboxService outboxService = Mockito.mock(DwIngestionOutboxService.class);
        DwIngestionJobQueue jobQueue = Mockito.mock(DwIngestionJobQueue.class);
        OutboxMessagePublisher publisher = Mockito.mock(OutboxMessagePublisher.class);
        DwIngestionOutboxRelay relay = newRelay(outboxService, jobQueue, publisher, 10);

        DwIngestionOutbox entry = new DwIngestionOutbox(com.example.dw.application.job.DwIngestionJobType.FETCH_NEXT,
                java.time.OffsetDateTime.now(), java.time.OffsetDateTime.now());
        entry.markSending(java.time.Clock.systemUTC(), "test");
        when(outboxService.claimPending(10)).thenReturn(List.of(entry));
        org.mockito.Mockito.doThrow(new IllegalStateException("pub fail")).when(publisher).publish(entry);

        relay.relay();

        verify(outboxService).markFailed(entry);
        org.assertj.core.api.Assertions.assertThat(relay.trigger()).isNotNull();
    }

    private DwIngestionOutboxRelay newRelay(DwIngestionOutboxService outboxService,
                                            DwIngestionJobQueue jobQueue,
                                            OutboxMessagePublisher publisher,
                                            int batchSize) {
        ObjectProvider<PolicySettingsProvider> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        return new DwIngestionOutboxRelay(outboxService, jobQueue, publisher, batchSize, 5_000, provider, false);
    }
}
