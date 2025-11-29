package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.policy.PolicyToggleSettings;
import com.example.common.masking.DataKind;

class MaskingServiceTest {

    @Test
    @DisplayName("허용된 역할이 forceUnmask 요청 시 감사 싱크가 호출된다")
    void unmaskAuditSinkCalled() {
        TestUnmaskSink sink = new TestUnmaskSink();
        PolicyToggleSettings settings = new PolicyToggleSettings(
                false, false, false,
                java.util.List.of(),
                20_000_000L,
                java.util.List.of("pdf"),
                false,
                0,
                true,
                true,
                true,
                730,
                true,
                "MEDIUM",
                true,
                java.util.List.of(),
                java.util.List.of("AUDIT_ADMIN")
        );
        PolicyMaskingStrategy strategy = new PolicyMaskingStrategy(settings);
        MaskingService service = new MaskingService(strategy, sink);

        MaskingTarget target = MaskingTarget.builder()
                .dataKind(DataKind.SSN)
                .defaultMask(true)
                .forceUnmask(true)
                .requesterRoles(Set.of("AUDIT_ADMIN"))
                .build();

        Maskable<String> maskable = new Maskable<String>() {
            @Override public String raw() { return "123456-7890123"; }
            @Override public String masked() { return "123456-1******"; }
            @Override public DataKind dataKind() { return DataKind.SSN; }
        };

        String result = service.render(maskable, target, "residentId");

        assertThat(result).isEqualTo("123456-7890123");
        assertThat(sink.called).isTrue();
        assertThat(sink.fieldName).isEqualTo("residentId");
    }

    static class TestUnmaskSink implements UnmaskAuditSink {
        boolean called = false;
        String fieldName;

        @Override
        public void handle(UnmaskAuditEvent event) {
            this.called = true;
            this.fieldName = event.getFieldName();
        }
    }
}
