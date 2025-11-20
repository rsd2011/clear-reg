package com.example.dwworker.queue;

import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.dw.application.job.DwIngestionJob;
import com.example.dw.application.job.DwIngestionJobQueue;
import com.example.dw.application.job.DwIngestionJobType;
import com.example.dw.application.job.DwIngestionOutboxEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "dw.ingestion.kafka", name = "enabled", havingValue = "true")
public class DwIngestionKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(DwIngestionKafkaConsumer.class);

    private final DwIngestionJobQueue jobQueue;
    private final ObjectMapper objectMapper;

    public DwIngestionKafkaConsumer(DwIngestionJobQueue jobQueue, ObjectMapper objectMapper) {
        this.jobQueue = jobQueue;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${dw.ingestion.outbox.publisher.kafka.topic:dw-ingestion-jobs}",
            groupId = "${dw.ingestion.kafka.group-id:dw-worker}",
            autoStartup = "${dw.ingestion.kafka.listener.auto-start:true}")
    public void onMessage(ConsumerRecord<String, String> record) {
        String payload = record.value();
        try {
            DwIngestionOutboxEvent event = objectMapper.readValue(payload, DwIngestionOutboxEvent.class);
            DwIngestionJob job = DwIngestionJob.fromOutbox(event.outboxId(), event.jobType());
            jobQueue.enqueue(job);
        }
        catch (Exception ex) {
            log.error("Failed to consume DW ingestion event from Kafka, key={} payload={} reason={}", record.key(), payload, ex.getMessage(), ex);
        }
    }
}
