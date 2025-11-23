package com.example.audit.infra.maintenance;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PostgreSQL 기준 월 단위 파티션을 사전 생성하는 스켈레톤.
 * HOT(최근 3개월)과 COLD(그 이전) 테이블로 분리할 수 있도록 확장 가능.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditPartitionScheduler {

    private final DataSource dataSource;
    private final Clock clock;

    /**
     * 매월 1일 02:00에 다음 달 파티션 생성 시도.
     */
    @Scheduled(cron = "0 0 2 1 * *")
    public void createNextMonthPartition() {
        LocalDate nextMonth = LocalDate.now(clock).plusMonths(1).withDayOfMonth(1);
        String suffix = nextMonth.format(DateTimeFormatter.ofPattern("yyyy_MM"));
        String partitionName = "audit_log_" + suffix;
        String ddl = """
                CREATE TABLE IF NOT EXISTS %s PARTITION OF audit_log
                FOR VALUES FROM ('%s') TO ('%s');
                """.formatted(partitionName,
                nextMonth,
                nextMonth.plusMonths(1));
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute(ddl);
            log.info("Audit partition ensured: {}", partitionName);
        } catch (SQLException e) {
            log.warn("Failed to create audit partition {}: {}", partitionName, e.getMessage());
        }
    }
}
