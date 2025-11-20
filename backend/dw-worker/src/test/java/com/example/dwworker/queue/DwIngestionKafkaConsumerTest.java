package com.example.dwworker.queue;

import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.example.dw.application.job.DwIngestionJob;
import com.example.dw.application.job.DwIngestionJobQueue;
import com.example.dw.application.job.DwIngestionJobType;
import com.example.dw.application.job.DwIngestionOutboxEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

class DwIngestionKafkaConsumerTest {

    private final DwIngestionJobQueue jobQueue = Mockito.mock(DwIngestionJobQueue.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DwIngestionKafkaConsumer consumer = new DwIngestionKafkaConsumer(jobQueue, objectMapper);

    @Test
    void consumesAndEnqueuesJob() throws Exception {
        UUID outboxId = UUID.randomUUID();
        DwIngestionOutboxEvent event = new DwIngestionOutboxEvent(outboxId, DwIngestionJobType.FETCH_NEXT, null);
        String payload = objectMapper.writeValueAsString(event);
        ConsumerRecord<String, String> record = new ConsumerRecord<>("dw-ingestion-jobs", 0, 0L, outboxId.toString(), payload);

        consumer.onMessage(record);

        ArgumentCaptor<DwIngestionJob> captor = ArgumentCaptor.forClass(DwIngestionJob.class);
        verify(jobQueue).enqueue(captor.capture());
        DwIngestionJob job = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(job.outboxId()).isEqualTo(outboxId);
        org.assertj.core.api.Assertions.assertThat(job.type()).isEqualTo(DwIngestionJobType.FETCH_NEXT);
    }
}
