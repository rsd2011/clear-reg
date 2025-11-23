package com.example.batch.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.common.policy.PolicyChangedEvent;
import com.example.common.policy.PolicySettingsProvider;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class AuditPartitionSchedulerPolicyEventTest {

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
    DataSource dataSource;

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    AuditPartitionScheduler scheduler;

    @Autowired
    PolicySettingsProvider policySettingsProvider;

    @Test
    @DisplayName("PolicyChangedEvent 수신 시 파티션 생성이 즉시 트리거된다")
    void refreshOnPolicyChangeCreatesPartition() throws Exception {
        // given: drop any existing partition for deterministic assertion
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS audit_log (id BIGINT)");
            st.execute("DROP TABLE IF EXISTS audit_log_2099_01");
        }

        // when: fire policy changed event
        publisher.publishEvent(new PolicyChangedEvent("security.policy"));

        // then: partition for next month (relative to Clock now) exists
        String suffix = Clock.systemUTC().instant().atZone(ZoneOffset.UTC).plusMonths(1)
                .withDayOfMonth(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy_MM"));
        String partitionName = "audit_log_" + suffix;
        try (Connection conn = dataSource.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, partitionName, null)) {
            assertThat(rs.next()).as("partition table should exist after policy change event").isTrue();
        }
    }
}
