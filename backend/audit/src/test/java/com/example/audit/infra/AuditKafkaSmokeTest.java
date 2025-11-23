package com.example.audit.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;

class AuditKafkaSmokeTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "AUDIT_KAFKA_BOOTSTRAP", matches = ".+")
    @DisplayName("실제 브로커에 메시지를 발행/수신할 수 있다")
    void publishAndConsume() {
        String bootstrap = System.getenv("AUDIT_KAFKA_BOOTSTRAP");
        String topic = "audit.events.v1";

        // producer
        Map<String, Object> producerProps = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap,
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.ACKS_CONFIG, "all");
        KafkaTemplate<String, String> template =
                new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));

        String eventId = UUID.randomUUID().toString();
        template.send(topic, eventId, "{\"ping\":\"pong\"}");
        template.flush();

        // consumer
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("audit-smoke", "false", bootstrap);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps)) {
            consumer.subscribe(java.util.List.of(topic));
            ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, topic, Duration.ofSeconds(5));
            assertThat(record).isNotNull();
            assertThat(record.key()).isEqualTo(eventId);
            assertThat(record.value()).contains("ping");
        }
    }
}
