package com.example.batch.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.common.policy.PolicySettingsProvider;
import com.example.common.policy.PolicyToggleSettings;

@Testcontainers
class AuditPartitionSchedulerContainerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    static DataSource dataSource;
    static JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void init() throws Exception {
        postgres.start();
        org.springframework.jdbc.datasource.SimpleDriverDataSource ds = new org.springframework.jdbc.datasource.SimpleDriverDataSource();
        ds.setDriver(new org.postgresql.Driver());
        ds.setUrl(postgres.getJdbcUrl());
        ds.setUsername(postgres.getUsername());
        ds.setPassword(postgres.getPassword());
        dataSource = ds;
        jdbcTemplate = new JdbcTemplate(ds);
        jdbcTemplate.execute("CREATE TABLE audit_log (event_time date) PARTITION BY RANGE (event_time);");
    }

    @AfterAll
    static void cleanup() {
        postgres.stop();
    }

    @Test
    @DisplayName("정책 변경 이벤트 후 파티션이 실제 Postgres에 생성된다")
    void createsPartitionInPostgresOnPolicyChange() throws Exception {
        AtomicReference<PolicyToggleSettings> ref = new AtomicReference<>(
                new PolicyToggleSettings(true, true, true, null, 0L, null, true, 0,
                        false, false, false, 0, true, "MEDIUM", true, null, null,
                        false, "0 0 2 1 * *", 0, true, "0 0 4 1 * *"));
        PolicySettingsProvider provider = new PolicySettingsProvider() {
            @Override public PolicyToggleSettings currentSettings() { return ref.get(); }
        };

        Clock clock = Clock.fixed(LocalDate.of(2025, 3, 10).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        AuditPartitionScheduler scheduler = new AuditPartitionScheduler(dataSource, clock, provider);
        // enable 파라미터 업데이트
        ref.set(new PolicyToggleSettings(true, true, true, null, 0L, null, true, 0,
                true, true, true, 0, true, "MEDIUM", true, null, null,
                true, "0 0 2 1 * *", 1, true, "0 0 4 1 * *"));

        scheduler.refreshOnPolicyChange(new AuditPartitionPolicyChangedEvent(AuditPartitionPolicyChangedEvent.AUDIT_POLICY_CODE));

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getTables(null, null, "audit_log_2025_04", null);
            assertThat(rs.next()).isTrue();
        }
    }
}
