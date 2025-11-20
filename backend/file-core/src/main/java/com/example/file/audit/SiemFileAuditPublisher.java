package com.example.file.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Placeholder SIEM 연계 퍼블리셔: 현재는 구조화 로그를 남기며,
 * 실제 SIEM 전송(HTTP/Syslog 등)은 후속 작업에서 어댑터 교체로 대응.
 */
@Component
@ConditionalOnProperty(name = "file.audit.publisher", havingValue = "siem")
public class SiemFileAuditPublisher implements FileAuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(SiemFileAuditPublisher.class);

    @Override
    public void publish(FileAuditEvent event) {
        log.info("siem-file-audit action={} fileId={} actor={} at={}", event.action(), event.fileId(), event.actor(), event.occurredAt());
    }
}
