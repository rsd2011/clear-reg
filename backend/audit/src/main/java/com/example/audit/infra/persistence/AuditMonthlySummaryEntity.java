package com.example.audit.infra.persistence;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 월간 감사 로그 요약 테이블 엔티티.
 * yearMonth는 yyyy-MM 문자열을 PK로 사용한다.
 */
@Entity
@Table(name = "audit_monthly_summary")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditMonthlySummaryEntity {

    @Id
    @Column(name = "year_month", length = 7, nullable = false)
    private String yearMonth; // e.g. 2025-01

    @Column(name = "total_count", nullable = false)
    private long totalCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
