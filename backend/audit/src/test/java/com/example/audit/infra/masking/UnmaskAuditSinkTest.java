package com.example.audit.infra.masking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import com.example.common.masking.MaskingService;
import com.example.common.masking.MaskingStrategy;
import com.example.common.masking.MaskingTarget;
import com.example.common.masking.UnmaskAuditSink;
import com.example.common.masking.UnmaskAuditEvent;
import com.example.common.masking.SubjectType;

@DataJpaTest
@ContextConfiguration(classes = UnmaskAuditSinkTest.Config.class)
@Import({JpaUnmaskAuditSink.class})
class UnmaskAuditSinkTest {

    @Autowired
    UnmaskAuditSink sink;

    @Autowired
    UnmaskAuditRepository repository;

    @Autowired
    MaskingService maskingService;

    @TestConfiguration
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableJpaRepositories(basePackages = "com.example.audit")
    @EntityScan(basePackages = {"com.example.audit", "com.example.common"})
    static class Config {
        @Bean
        MaskingStrategy maskingStrategy() {
            return target -> true; // always mask => unmask events rely on sink handle directly
        }

        @Bean
        MaskingService maskingService(MaskingStrategy strategy, UnmaskAuditSink sink) {
            return new MaskingService(strategy, sink);
        }
    }

    @Test
    @DisplayName("unmask 발생 시 JPA 싱크가 레코드를 저장한다")
    void unmaskPersisted() {
        MaskingTarget target = MaskingTarget.builder()
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind("RRN")
                .forceUnmask(true)
                .build();

        // 직접 싱크 호출 (MaskingService는 전략에 따라 raw 반환 시 sink 호출)
        sink.handle(UnmaskAuditEvent.builder()
                .eventTime(Instant.now())
                .subjectType(target.getSubjectType())
                .dataKind(target.getDataKind())
                .fieldName("residentId")
                .rowId("ROW-1")
                .build());

        assertThat(repository.findAll()).hasSize(1);
        UnmaskAuditRecord record = repository.findAll().getFirst();
        assertThat(record.getSubjectType()).isEqualTo(SubjectType.CUSTOMER_INDIVIDUAL);
        assertThat(record.getDataKind()).isEqualTo("RRN");
        assertThat(record.getFieldName()).isEqualTo("residentId");
    }
}
