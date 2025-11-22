package com.example.audit.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.Mockito;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import com.example.audit.Actor;
import com.example.audit.ActorType;
import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPolicySnapshot;
import com.example.audit.RiskLevel;
import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.audit.infra.policy.AuditPolicyResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * 실 브로커 smoke: AUDIT_KAFKA_BOOTSTRAP 환경변수가 있어야 동작.
 * 브로커/DLQ 토픽이 없으면 자동으로 생성한 뒤 발행 후 수신을 검증한다.
 */
@EnabledIfEnvironmentVariable(named = "AUDIT_KAFKA_BOOTSTRAP", matches = ".+")
class AuditKafkaSmokeTest {

    @Test
    void smoke_publishAndConsume_realBroker() throws Exception {
        String bootstrap = System.getenv("AUDIT_KAFKA_BOOTSTRAP");

        try (AdminClient admin = AdminClient.create(Map.of(
                org.apache.kafka.clients.admin.AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap))) {
            admin.createTopics(java.util.List.of(
                    new NewTopic("audit.events.v1", 1, (short) 1),
                    new NewTopic("audit.events.dlq", 1, (short) 1))).all().get();
        }

        Map<String, Object> producerProps = KafkaTestUtils.producerProps(bootstrap);
        producerProps.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        KafkaTemplate<String, String> template = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProps));

        var repository = Mockito.mock(AuditLogRepository.class);
        var resolver = Mockito.mock(AuditPolicyResolver.class);
        Mockito.when(resolver.resolve(Mockito.any(), Mockito.any()))
                .thenReturn(java.util.Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        AuditRecordService service = new AuditRecordService(repository, resolver, mapper, template, "audit.events.v1", false, "", "default", new com.example.audit.infra.masking.MaskingProperties());

        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID())
                .eventTime(Instant.now())
                .eventType("SMOKE")
                .moduleName("audit")
                .action("BROKER_TEST")
                .actor(Actor.builder().id("smoke-user").type(ActorType.HUMAN).build())
                .riskLevel(RiskLevel.LOW)
                .build();

        service.record(event, AuditMode.ASYNC_FALLBACK);
        template.flush();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("audit-smoke-consumer", "false", bootstrap);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        var consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        var consumer = consumerFactory.createConsumer();
        consumer.subscribe(java.util.List.of("audit.events.v1"));

        var record = KafkaTestUtils.getSingleRecord(consumer, "audit.events.v1", Duration.ofSeconds(15));
        assertThat(record).isNotNull();
        assertThat(record.key()).isEqualTo(event.getEventId().toString());
    }
}
