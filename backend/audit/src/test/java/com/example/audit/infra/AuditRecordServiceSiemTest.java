package com.example.audit.infra;

import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.audit.AuditEvent;
import com.example.audit.AuditMode;
import com.example.audit.AuditPolicySnapshot;
import com.example.audit.infra.persistence.AuditLogRepository;
import com.example.audit.infra.policy.AuditPolicyResolver;
import com.example.audit.infra.siem.SiemForwarder;
import com.example.audit.infra.masking.MaskingProperties;
import com.example.common.masking.MaskingService;
import com.fasterxml.jackson.databind.ObjectMapper;

class AuditRecordServiceSiemTest {

    @Test
    @DisplayName("SIEM 포워더가 호출된다")
    void siemForwarded() {
        AuditLogRepository repo = Mockito.mock(AuditLogRepository.class);
        AuditPolicyResolver resolver = Mockito.mock(AuditPolicyResolver.class);
        Mockito.when(resolver.resolve(Mockito.any(), Mockito.any()))
                .thenReturn(Optional.of(AuditPolicySnapshot.builder().enabled(true).build()));
        SiemForwarder siem = Mockito.mock(SiemForwarder.class);

        AuditRecordService service = new AuditRecordService(
                repo, resolver, new ObjectMapper(), null,
                "audit.events.v1", "", null,
                false, "", "kid", new MaskingProperties(), new MaskingService(t -> false), siem);

        AuditEvent event = AuditEvent.builder()
                .eventId(UUID.randomUUID())
                .eventTime(Instant.now())
                .eventType("TEST")
                .action("SIEM")
                .build();

        service.record(event, AuditMode.ASYNC_FALLBACK);

        verify(siem).forward(Mockito.any());
    }
}
