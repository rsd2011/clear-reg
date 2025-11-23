package com.example.audit.infra.maintenance;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.audit.infra.persistence.AuditLogRepository;

/**
 * 월간 접속/감사 로그 점검 리포트를 생성하기 위한 스켈레톤.
 * 현재는 건수만 수집해 로그로 남기며, 향후 SIEM/Grafana 연계 시 데이터를 내보내도록 확장한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditMonthlyReportJob {

    private final AuditLogRepository repository;
    private final Clock clock;

    /**
     * 매월 1일 04:00에 지난달 로그 건수 리포트.
     */
    @Scheduled(cron = "0 0 4 1 * *")
    public void report() {
        LocalDate now = LocalDate.now(clock);
        LocalDate start = now.minusMonths(1).withDayOfMonth(1);
        LocalDate end = now.withDayOfMonth(1);
        long count = repository.count(); // TODO: 기간 필터 집계로 확장
        log.info("Audit monthly report {} ~ {} count={}", start, end.minusDays(1), count);
    }
}
