package com.example.audit.infra.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.example.common.masking.DataKind;
import com.example.common.masking.MaskingService;
import com.example.common.masking.MaskingStrategy;
import com.example.common.masking.MaskingTarget;
import com.example.common.masking.PolicyMaskingStrategy;
import com.example.common.masking.SubjectType;
import com.example.common.masking.UnmaskAuditSink;
import com.example.common.policy.PolicyToggleSettings;

@DataJpaTest(properties = "spring.main.allow-bean-definition-overriding=true")
@Import({JpaUnmaskAuditSink.class, MaskingServiceUnmaskE2ETest.Config.class})
class MaskingServiceUnmaskE2ETest {

    @TestConfiguration
    @EnableJpaRepositories(basePackages = "com.example.audit")
    @EntityScan(basePackages = {"com.example.audit", "com.example.common"})
    static class Config {
        @Bean
        PolicyToggleSettings policyToggleSettings() {
            return new PolicyToggleSettings(
                    false, false, false,
                    List.of(),
                    20_000_000L,
                    List.of("pdf"),
                    false,
                    0,
                    true,
                    true,
                    true,
                    730,
                    true,
                    "MEDIUM",
                    true,
                    List.of(),
                    List.of("AUDIT_ADMIN")
            );
        }

        @Bean
        MaskingService maskingService(PolicyToggleSettings settings, UnmaskAuditSink sink) {
            return new MaskingService(new PolicyMaskingStrategy(settings), sink);
        }
    }

    @org.springframework.beans.factory.annotation.Autowired
    MaskingService maskingService;

    @org.springframework.beans.factory.annotation.Autowired
    UnmaskAuditRepository repository;

    @Test
    @DisplayName("MaskingService render 시 forceUnmask + 허용 역할이면 UnmaskAudit이 DB에 적재된다")
    void renderTriggersAuditPersist() {
        // given
        var target = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind(DataKind.SSN)
                .forceUnmask(true)
                .requesterRoles(java.util.Set.of("AUDIT_ADMIN"))
                .rowId("ROW-123")
                .build();

        com.example.common.masking.Maskable maskable = new com.example.common.masking.Maskable() {
            @Override public String raw() { return "123456-7890123"; }
            @Override public String masked() { return "123456-1******"; }
        };

        // when
        String rendered = maskingService.render(maskable, target, "residentId");

        // then
        assertThat(rendered).isEqualTo("123456-7890123");
        assertThat(repository.findAll()).hasSize(1);
        UnmaskAuditRecord record = repository.findAll().getFirst();
        assertThat(record.getSubjectType()).isEqualTo(SubjectType.CUSTOMER_INDIVIDUAL);
        assertThat(record.getDataKind()).isEqualTo("SSN");
        assertThat(record.getFieldName()).isEqualTo("residentId");
        assertThat(record.getRowId()).isEqualTo("ROW-123");
        assertThat(record.getEventTime()).isNotNull();
    }
}
