package com.example.audit.infra.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * DLQ(audit.events.dlq)로 떨어진 감사 이벤트를 주 토픽으로 재전송한다.
 * 브로커/토픽이 준비된 환경에서만 활성화되도록 프로퍼티로 토글한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "audit.kafka.dlq", name = "enabled", havingValue = "true")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Spring KafkaTemplate DI 주입")
public class AuditDlqReprocessor {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AuditKafkaProperties props;

    @KafkaListener(topics = "${audit.kafka.dlq-topic:audit.events.dlq}",
            groupId = "${audit.kafka.dlq-group:audit-dlq-reprocessor}")
    public void handleDlq(@Payload String payload,
                          @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        String eventKey = key != null ? key : "";
        try {
            kafkaTemplate.send(props.getTopic(), eventKey, payload);
            log.info("Requeued audit event from DLQ key={}", eventKey);
        } catch (Exception e) {
            log.warn("Failed to requeue audit event from DLQ key={}: {}", eventKey, e.getMessage());
        }
    }
}
