package com.example.common.schedule;

import java.time.ZoneId;
import java.util.Map;

/**
 * 기본 정책/프로퍼티에서 사용하는 배치 잡 스케줄 기본값 모음.
 * 중앙 스케줄러 및 PolicyToggleSettings 초기값으로 재사용한다.
 */
public final class BatchJobDefaults {

    private BatchJobDefaults() {}

    public static Map<BatchJobCode, BatchJobSchedule> defaults() {
        return Map.of(
                BatchJobCode.FILE_SECURITY_RESCAN, new BatchJobSchedule(true, TriggerType.FIXED_DELAY, null, 60_000, 0, null),
                BatchJobCode.FILE_AUDIT_OUTBOX_RELAY, new BatchJobSchedule(true, TriggerType.FIXED_DELAY, null, 5_000, 0, null),
                BatchJobCode.DW_INGESTION_OUTBOX_RELAY, new BatchJobSchedule(true, TriggerType.FIXED_DELAY, null, 5_000, 0, null),
                BatchJobCode.DRAFT_AUDIT_OUTBOX_RELAY, new BatchJobSchedule(true, TriggerType.FIXED_DELAY, null, 60_000, 0, null),
                BatchJobCode.AUDIT_PARTITION_PRECREATE, new BatchJobSchedule(true, TriggerType.CRON, "0 0 2 1 * *", 0, 0, null),
                BatchJobCode.AUDIT_COLD_MAINTENANCE, new BatchJobSchedule(true, TriggerType.CRON, "0 0 5 * * 0", 0, 0, null),
                BatchJobCode.AUDIT_ARCHIVE, new BatchJobSchedule(true, TriggerType.CRON, "0 30 3 2 * *", 0, 0, null),
                BatchJobCode.AUDIT_LOG_RETENTION, new BatchJobSchedule(true, TriggerType.CRON, "0 0 3 * * *", 0, 0, null),
                BatchJobCode.AUDIT_MONTHLY_REPORT, new BatchJobSchedule(true, TriggerType.CRON, "0 0 4 1 * *", 0, 0, null),
                BatchJobCode.AUDIT_COLD_ARCHIVE_SCHEDULER, new BatchJobSchedule(true, TriggerType.CRON, "0 30 2 2 * *", 0, 0, null)
        );
    }
}
