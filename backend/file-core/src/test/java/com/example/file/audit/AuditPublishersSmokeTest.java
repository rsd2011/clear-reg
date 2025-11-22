package com.example.file.audit;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class AuditPublishersSmokeTest {

    private static final FileAuditEvent EVENT = new FileAuditEvent("UPLOAD", UUID.randomUUID(), "actor", OffsetDateTime.now());

    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Kafka 퍼블리셔는 serialize 후 전송한다")
    void kafkaPublisherSends() {
        KafkaFileAuditPublisher publisher = new KafkaFileAuditPublisher(kafkaTemplate, "topic");

        assertThatNoException().isThrownBy(() -> publisher.publish(EVENT));
        verify(kafkaTemplate).send("topic", EVENT.fileId().toString(), AuditJsonSerializer.serialize(EVENT));
    }

    @Test
    @DisplayName("Outbox 퍼블리셔는 insert 쿼리를 실행한다")
    void outboxPublisherInsertsRow() {
        OutboxFileAuditPublisher publisher = new OutboxFileAuditPublisher(jdbcTemplate);

        assertThatNoException().isThrownBy(() -> publisher.publish(EVENT));
        // insert 쿼리 실행 여부만 검증 (파라미터는 anyObject 매칭)
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("SIEM 퍼블리셔는 로그를 남기고 예외를 던지지 않는다")
    void siemPublisherLogs() {
        SiemFileAuditPublisher publisher = new SiemFileAuditPublisher();

        assertThatNoException().isThrownBy(() -> publisher.publish(EVENT));
    }
}
