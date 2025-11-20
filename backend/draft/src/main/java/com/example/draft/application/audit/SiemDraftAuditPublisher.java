package com.example.draft.application.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * SIEM 연동용 퍼블리셔 (카프카/시스로그 대체 가능). 현재는 전용 로거로 출력한다.
 */
@Component
@ConditionalOnProperty(name = "draft.audit.publisher", havingValue = "siem")
public class SiemDraftAuditPublisher implements DraftAuditPublisher {

    private static final Logger SIEM_LOGGER = LoggerFactory.getLogger("SIEM_AUDIT");

    @Override
    public void publish(DraftAuditEvent event) {
        SIEM_LOGGER.info("action={} draftId={} actor={} org={} ip={} ua={} at={}",
                event.action(),
                event.draftId(),
                event.actor(),
                event.organizationCode(),
                event.ip(),
                event.userAgent(),
                event.occurredAt());
    }
}
