package com.example.draft.application.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import com.example.draft.domain.DraftAction;

@DisplayName("SpringDraftAuditPublisher는 이벤트를 퍼블리셔에 위임한다")
class SpringDraftAuditPublisherTest {

    @Test
    void publishDelegatesToEventPublisher() {
        ApplicationEventPublisher publisher = Mockito.mock(ApplicationEventPublisher.class);
        SpringDraftAuditPublisher auditPublisher = new SpringDraftAuditPublisher(publisher);
        DraftAuditEvent event = new DraftAuditEvent(DraftAction.SUBMITTED, UUID.randomUUID(), "actor",
                "ORG", "comment", "ip", "ua", OffsetDateTime.now());

        auditPublisher.publish(event);

        verify(publisher).publishEvent(any(DraftAuditEvent.class));
    }
}
