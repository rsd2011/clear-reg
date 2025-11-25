# Observability Overview (Consolidated)

## From: observability.md

# Observability & Actuator Endpoints

## Actuator 개요
- `backend/server`와 `backend/batch-app` 모두 `spring-boot-starter-actuator`를 포함하며 동일한 관리 설정을 사용한다.
- 노출되는 기본 HTTP 엔드포인트: `/actuator/health`, `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus` (기본 서버 포트 기준).
- 모든 엔드포인트는 `management.endpoints.web.exposure.include=health,info,metrics,prometheus` 로 제어되며, 추가 노출이 필요할 경우 동일 리스트에 추가한다.

## 보안/디테일 정책
- `management.endpoint.health.show-details=when_authorized` 설정으로 인증된 사용자인 경우에만 상세 health 정보를 확인할 수 있다 (기본 Spring Security 설정과 동일한 인증 체계를 따른다).
- `management.metrics.tags.application=${spring.application.name}` 로 각 모듈 이름을 메트릭 태그에 자동 포함하여 Prometheus/Grafana에서 서비스별 지표를 구분할 수 있다.

## 서버 모듈 (`backend/server`)
- 포트: 애플리케이션 포트(기본 8080)와 동일.
- 대표 사용 사례: API 인스턴스 health 체크, HTTP 서버 레이턴시/에러율 모니터링, `prometheus` scrape.
- 향후 필요 시 `management.server.port` 를 별도로 정의해 전용 포트로 뽑을 수 있다.

## 배치 모듈 (`backend/batch-app`)
- 포트: 기본 애플리케이션 포트(구동 시 설정)와 동일.
- Health/metrics 를 통해 Quartz 스케줄러, DW ingestion 처리량 등을 외부 모니터링 도구에 노출할 수 있다.

## Prometheus 스크랩 예시
```yaml
- job_name: 'clear-reg-server'
  metrics_path: '/actuator/prometheus'
  static_configs:
    - targets: ['server-host:8080']

- job_name: 'clear-reg-batch'
  metrics_path: '/actuator/prometheus'
  static_configs:
    - targets: ['batch-host:8080']
```

## TODO
- 향후 `management.endpoint.health.probes.enabled=true` 등을 추가해 Kubernetes liveness/readiness probe를 직접 노출하는 것도 고려한다.
- Trace/metrics export(OpenTelemetry, Micrometer Registry) 연계를 위한 설정은 `management.metrics.export.*` 블록에서 확장 가능.
- Draft/Approval 피쳐 관측성:
  - 주요 비즈니스 메트릭: 결재 리드타임, 반려율, 회수/재상신 건수, 알림 발송 성공률. 향후 지표 스키마를 `docs/draft-approval-todo.md` T12·T16 진행 시 함께 정의.
  - OTLP 설정 참고: `application-otlp.yml` 프로파일을 Draft API에도 적용하여 trace/metric/log 삼중 수집.
  - 알림/이벤트 outbox 처리율·실패율을 Prometheus 커스텀 메트릭으로 노출 (T12 연계).
\n---\n
## From: monitoring/audit-monthly-report-grafana.md

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
\n---\n
## From: observability/grafana-dashboard-plan.md

# Grafana Dashboard Plan

본 문서는 `clear-reg` 시스템 전반을 감시하기 위한 Grafana 대시보드 구성을 정의한다. 총 4개의 패널 그룹을 기본으로 제공하며, 템플릿 변수로 `application`(server/batch), `instance`, `environment`를 사용한다.

## 템플릿 변수 설정
- `application`: Prometheus 메트릭 `application` 태그 기반 (`multi-module-application`, `batch-app`).
- `instance`: 서버 IP:포트 혹은 Kubernetes Pod.
- `environment`: prometheus relabel을 통해 주입된 `env` 라벨 (dev/stage/prod).

## 대시보드 구조

### 1. Auth/API 패널 그룹
1. **API Error Rate**
   - Query: `sum(rate(http_server_requests_seconds_count{application="multi-module-application",status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count{application="multi-module-application"}[5m]))`
   - Viz: Time series + single stat (%).
2. **JWT Failures by Reason**
   - Query: `sum(rate(security_auth_failed_total{reason=~"TOKEN_.*",application="multi-module-application"}[5m])) by (reason)`
   - Viz: Stack Bar/Time series.
3. **Latency Histogram (p50/p95/p99)**
   - Query: `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{application="multi-module-application"}[5m])) by (le))`
   - Viz: Multi-line chart.

### 2. DW Ingestion 패널 그룹
1. **Batch Success/Failure Count**
   - Metrics: `dw_ingestion_failures_total`, `dw_ingestion_success_total` (custom meter 추가 시).
   - Viz: Bar chart last 24h.
2. **Ingestion Duration**
   - Query: `avg_over_time(dw_ingestion_duration_seconds_sum[1h]) / avg_over_time(dw_ingestion_duration_seconds_count[1h])`
   - Viz: Time series.
3. **Quartz Trigger Lag**
   - Query: `quartz_trigger_misfire_total` (rate) + `quartz_jobs_running`
   - Viz: Table/Single stat.

### 3. Cache & Infra 패널 그룹
1. **Redis Memory Usage**
   - Query: `redis_memory_used_bytes / redis_maxmemory_bytes`
   - Viz: Gauge (threshold 80%, 90%).
2. **Caffeine Hit Ratio**
   - Query: `avg_over_time(cache_hit_ratio{application="$application",cache=~"DW_.*"}[10m])`
   - Viz: Table by cache name.
3. **Evictions Trend**
   - Query: `increase(cache_evictions_total{application="$application"}[30m])`
   - Viz: Bar chart.

### 4. Permission / Security 패널 그룹
1. **RequirePermission Denials**
   - Query: `sum(rate(permission_denied_total{application="$application"}[5m])) by (feature)` (custom meter 필요).
   - Viz: Pie chart.
2. **Masking/Unmask Requests**
   - Query: `sum(rate(masking_action_total{action=~"UNMASK|MASK"}[5m])) by (action)`
   - Viz: Time series.

## 이벤트/Annotation
- Deploy 이벤트: GitHub Actions Webhook → Grafana annotation API 호출 (commit SHA, PR 링크).
- DW 배치 재시작: `/api/dw/batches/ingest` 호출 시 annotation 추가.

## 사용자 역할
- Ops: 전체 편집 권한.
- Dev/QA: View-only.
- Stakeholder: View-only, 특정 패널만 Embed 허용.

## 구현 체크리스트
1. Prometheus에서 필요한 메트릭 노출 (`cache_hit_ratio`, `dw_ingestion_*`, `permission_denied_total` 등) 여부 확인 후 exporter 추가.
2. Grafana JSON 스키마(Version 10.x)로 대시보드 정의 파일 생성, GitOps repo에 저장.
3. Alert rule도 Grafana/Alertmanager에서 동일 지표 기반으로 공유.
