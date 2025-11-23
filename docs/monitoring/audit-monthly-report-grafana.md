# Audit 월간 접속기록 대시보드 템플릿 (Grafana 예시)

## 요구 지표
- total_access_count : 월간 총 접속/조회 건수
- fail_ratio         : 실패 비율 (fail / total)
- night_access_count : 심야(00-06시) 조회 건수
- drm_download_count : DRM 해제/다운로드 시도 건수
- unmask_request_count : 마스킹 해제 요청 건수

## 예시 쿼리 (Prometheus → Loki 라벨 변환 가정)
```
sum(rate(audit_access_total[30d]))
sum(rate(audit_access_failed_total[30d])) / sum(rate(audit_access_total[30d]))
sum(rate(audit_drm_download_total[30d]))
sum(rate(audit_unmask_request_total[30d]))
```

## 패널 구성
1. Stat: Total Access (전월 대비 증감)
2. Stat: Fail Ratio (%)
3. Stat: Night Access Count
4. Stat: DRM Download Attempts
5. Stat: Unmask Requests
6. Table: 최근 10개 위험 이벤트 (risk_level=HIGH)
7. Alert: fail_ratio > 5% OR night_access_count > 500 → Slack/PagerDuty

## 알림 훅 템플릿 (Slack)
```
[AUDIT MONTHLY] total={{total}} fail_ratio={{fail_ratio}} night={{night}}
drm={{drm}} unmask={{unmask}} link={{dashboard_url}}
```

## 배치 연계
- `AuditMonthlyReportJob` 완료 시 summary를 Prometheus Pushgateway 또는 DB→Grafana 쿼리 소스로 적재.
- 알림은 Alertmanager 룰 또는 배치 완료 후 Webhook 호출로 발송.
