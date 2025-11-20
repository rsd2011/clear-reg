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
