package com.example.server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.draft.application.audit.DraftAuditEvent;
import com.example.draft.application.audit.DraftAuditPublisher;

@Configuration
public class TestDraftAuditConfig {

    @Bean
    DraftAuditPublisher draftAuditPublisher() {
        return new DraftAuditPublisher() {
            @Override
            public void publish(DraftAuditEvent event) {
                // no-op for tests
            }
        };
    }
}
