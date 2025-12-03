package com.example.batch.worker.queue;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.dw.application.job.DwIngestionJobQueue;
import com.fasterxml.jackson.databind.ObjectMapper;

class DwIngestionKafkaConsumerErrorTest {

    private final DwIngestionJobQueue jobQueue = Mockito.mock(DwIngestionJobQueue.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DwIngestionKafkaConsumer consumer = new DwIngestionKafkaConsumer(jobQueue, objectMapper);

    @Test
    @DisplayName("잘못된 JSON 페이로드는 큐에 넣지 않고 에러를 로깅한다")
    void skipsInvalidJsonPayload() {
        // Given
        ConsumerRecord<String, String> record = new ConsumerRecord<>("dw-ingestion-jobs", 0, 0L, "key", "not-json");

        // When
        consumer.onMessage(record);

        // Then
        verify(jobQueue, never()).enqueue(Mockito.any());
    }

    @Test
    @DisplayName("빈 페이로드는 무시하고 큐에 넣지 않는다")
    void skipsBlankPayload() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("dw-ingestion-jobs", 0, 0L, "key", "   ");

        consumer.onMessage(record);

        verify(jobQueue, never()).enqueue(Mockito.any());
    }

    @Test
    @DisplayName("jobType이 없는 이벤트는 큐에 넣지 않는다")
    void skipsWhenJobTypeMissing() throws Exception {
        String payload = "{\"outboxId\":\"" + java.util.UUID.randomUUID() + "\",\"jobType\":null}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("dw-ingestion-jobs", 0, 0L, "key", payload);

        consumer.onMessage(record);

        verify(jobQueue, never()).enqueue(Mockito.any());
    }
}
