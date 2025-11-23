package com.example.audit.infra.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "audit.kafka")
@Data
public class AuditKafkaProperties {
    /**
     * 메인 감사 토픽.
     */
    private String topic = "audit.events.v1";
    /**
     * DLQ 토픽. 비워두면 DLQ 재처리 비활성화.
     */
    private String dlqTopic = "audit.events.dlq";
    /**
     * DLQ 재처리 활성화 여부.
     */
    private boolean dlqEnabled = false;
    /**
     * DLQ consumer 그룹.
     */
    private String dlqGroup = "audit-dlq-reprocessor";
}
