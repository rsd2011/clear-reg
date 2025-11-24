package com.example.common.schedule;

import java.time.Instant;

public interface ScheduledJobPort {

    /**
     * 고유 식별자 (모니터링/로그 용도)
     */
    String jobId();

    /**
     * 실행 트리거 정의 (cron 또는 fixed-delay)
     */
    TriggerDescriptor trigger();

    /**
     * 작업 본문. 중앙 스케줄러가 트리거될 때마다 호출된다.
     */
    void runOnce(Instant now);
}
