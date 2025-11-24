package com.example.batch.audit;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * HOT 보존 기간이 지난 파티션을 COLD 테이블스페이스로 이동하고 재색인하는 경량 잡.
 * 파티션 이름 규칙: audit_log_yyyy_MM
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditColdMaintenanceJob {

    private final DataSource dataSource;
    private final Clock clock;

    @Value("${audit.partition.tablespace.cold:}")
    private String coldTablespace;

    @Value("${audit.partition.hot-months:6}")
    private int hotMonths;

    /**
     * 주 1회 실행: HOT 기간을 지난 지난달 파티션을 cold TS로 이동.
     */
    @Scheduled(cron = "${audit.cold-maintenance.cron:0 0 5 * * 0}")
    public void moveOldPartitions() {
        if (coldTablespace == null || coldTablespace.isBlank()) {
            return;
        }
        LocalDate targetMonth = LocalDate.now(clock).minusMonths(hotMonths + 1).withDayOfMonth(1);
        String suffix = targetMonth.format(DateTimeFormatter.ofPattern("yyyy_MM"));
        String partition = "audit_log_" + suffix;
        String alter = "ALTER TABLE %s SET TABLESPACE %s".formatted(partition, coldTablespace);
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute(alter);
            log.info("[audit-cold] moved {} to tablespace {}", partition, coldTablespace);
            try {
                conn.createStatement().execute("REINDEX TABLE " + partition);
                log.info("[audit-cold] reindex {}", partition);
            } catch (SQLException e) {
                log.warn("[audit-cold] reindex failed {}: {}", partition, e.getMessage());
            }
        } catch (SQLException e) {
            log.warn("[audit-cold] move failed {}: {}", partition, e.getMessage());
        }
    }
}
