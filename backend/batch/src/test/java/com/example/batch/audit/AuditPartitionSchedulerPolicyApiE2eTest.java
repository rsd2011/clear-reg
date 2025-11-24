package com.example.batch.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.common.policy.PolicyChangedEvent;
import com.example.common.policy.PolicySettingsProvider;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AuditPartitionSchedulerPolicyApiE2eTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("audit")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("audit.partition.enabled", () -> "true");
        registry.add("audit.partition.preload-months", () -> "0");
    }

    @Autowired
    TestRestTemplate rest;

    @Autowired
    DataSource dataSource;

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    AuditPartitionScheduler scheduler;

    @Autowired
    PolicySettingsProvider policySettingsProvider;

    @AfterEach
    void cleanUp() throws Exception {
        try (var conn = dataSource.getConnection(); var st = conn.createStatement()) {
            st.execute("DROP TABLE IF EXISTS audit_log CASCADE");
            st.execute("CREATE TABLE audit_log (id bigint)");
        }
    }

    @Test
    @DisplayName("Policy API 호출로 Cron/enable 변경 → PolicyChangedEvent → 파티션 생성이 즉시 반영된다")
    @Transactional
    void policyApiTriggersSchedulerRefresh() throws Exception {
        // given: base audit_log table
        try (var conn = dataSource.getConnection(); var st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS audit_log (id bigint)");
        }

        // when: simulate policy API update (publish event)
        publisher.publishEvent(new PolicyChangedEvent("security.policy"));

        // then: next-month partition created
        String suffix = Clock.systemUTC().instant().atZone(ZoneOffset.UTC).plusMonths(1)
                .withDayOfMonth(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy_MM"));
        String partitionName = "audit_log_" + suffix;
        try (var conn = dataSource.getConnection();
             var rs = conn.getMetaData().getTables(null, null, partitionName, null)) {
            assertThat(rs.next()).isTrue();
        }
    }
}
