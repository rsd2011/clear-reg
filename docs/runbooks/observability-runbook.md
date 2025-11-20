# Observability Runbook

이 문서는 주요 경보 시나리오( DW 적재 실패, 캐시 포화, JWT 토큰 오류 ) 발생 시 대응 절차와 추천 임계치를 정리한다. 모든 알람은 `/actuator/metrics`와 `/actuator/prometheus`에서 수집 가능한 지표를 기반으로 한다.

## 공통 모니터링 원칙
- 모든 경보에는 **Slack #ops-clear-reg** 알림 및 PagerDuty 미리 정의된 룰을 연동한다.
- 지표 수집 주기: Prometheus 15초. 경보 판정은 5분 이동 윈도우.
- 알람 발생 시 즉시 Runbook 링크와 최근 배포 SHA, 장애 티켓 생성 링크(Jira `OPS`)를 포함한다.

## 1. DW 적재 실패 (DW Ingestion Failure)
- **지표**
  - `dw_ingestion_failures_total` (Counter): `DwIngestionService`에서 `ingestNextFile` 실패 시 증가하도록 추후 custom meter 연동 예정. 현재는 Quartz Job 로그 기반.
  - `dw_ingestion_duration_seconds` (Timer/Histogram): 배치가 완료되기까지 소요 시간.
  - `dw_import_batches{status="FAILED"}` (Gauge): 최근 실패 배치 수 (DB/Prometheus exporter 필요).
- **알람 조건(권장)**
  - `increase(dw_ingestion_failures_total[5m]) >= 1`
  - 또는 `rate(dw_import_batches{status="FAILED"}[15m]) > 0.1`
  - Quartz Trigger misfire: Prometheus `quartz_trigger_misfire_total` >= 1.
- **대응 절차**
  1. `GET /actuator/health` → `dwIngestionScheduler` 컴포넌트 상태 확인.
  2. Kibana/CloudWatch에서 `DwIngestionService` ERROR 로그 필터(`log.logger=DW_INGESTION`).
  3. 실패 배치 ID 확인 후 `/api/dw/batches/{id}` 조회, 필요 시 `/api/dw/batches/ingest` 로 재시도.
  4. 원인(파일 스키마, DB 연결 등) 파악 후 Jira OPS 티켓 업데이트.
- **복구 기준**: 재시도 성공 및 `dw_ingestion_failures_total` 증가 중단.

## 2. 캐시 포화(Cache Saturation)
- **지표**
  - Caffeine: `cache_evictions_total{cache="DW_ORG_TREE"}`
  - Redis: `redis_memory_used_bytes`
  - Custom gauge: `cache_hit_ratio{cache="..."}` (Micrometer CacheMeterBinder 사용 권장).
- **알람 조건**
  - `cache_hit_ratio < 0.80` 가 10분 이상 지속.
  - `redis_memory_used_bytes / redis_maxmemory_bytes > 0.9`.
  - `cache_evictions_total` 증가율이 5분 동안 100 이상.
- **대응 절차**
  1. `/api/admin/policies/caches/clear` 사용 기록 확인 (불필요한 flush 여부 체크).
  2. 대량 요청/배치가 있는지 `DwOrganizationQueryService` 호출량 확인.
  3. Redis/애플리케이션 노드에 추가 메모리 할당 혹은 TTL 조정 (`cache.*` 프로퍼티) 후 재배포.
  4. 필요 시 캐시 무효화 정책/RowScope 캐시 크기 재설계.
- **복구 기준**: Hit ratio 회복(>0.9) 및 메모리 사용률 80% 이하.

## 3. JWT 토큰 오류 폭증(Token Error Spike)
- **지표**
  - `security_auth_failed_total{reason="TOKEN_EXPIRED"}`
  - `security_auth_failed_total{reason="TOKEN_SIGNATURE"}`
  - HTTP 401/403 비율 (`http_server_requests_seconds_count{status="401"}`)
- **알람 조건**
  - 5분 동안 `security_auth_failed_total` 증가량이 baseline 대비 3배 이상.
  - `rate(http_server_requests_seconds_count{status="401"}[5m]) > 0.2`.
- **대응 절차**
  1. 최근 배포/Secret Rotation 여부 확인 (JWT Secret mismatch 가능).
  2. `/actuator/health`의 `jwtSigner` 커스텀 헬스체크(추후 추가 예정) 확인.
  3. 문제가 secret mismatch라면 `security.jwt.secret` 를 양쪽 서버에 동일하게 재배포.
  4. 만료 폭증 시 Refresh Token 만료 정책 및 클라이언트 시간 동기화 상태 검토.
- **복구 기준**: 실패 비율이 정상화(401 rate <= 5%).

## 알람 설정 예시 (Prometheus Alertmanager)
```yaml
- alert: DwIngestionFailure
  expr: increase(dw_ingestion_failures_total[5m]) >= 1
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "DW ingestion failure detected"
    runbook: "docs/runbooks/observability-runbook.md#1-dw-적재-실패"

- alert: CacheSaturation
  expr: avg_over_time(cache_hit_ratio{cache=~"DW_.*"}[10m]) < 0.8
  for: 10m
  labels:
    severity: warning
  annotations:
    summary: "DW cache hit ratio degraded"
    runbook: "docs/runbooks/observability-runbook.md#2-캐시-포화cache-saturation"
```

## 향후 작업
- 커스텀 Micrometer Meter 등록 (DW ingestion 실패 카운터, 권한 실패 카운터 등) → 위 지표 실시간 확보.
- PagerDuty / Slack 자동 티켓 생성 파이프라인 구축 시 본 Runbook 링크 포함.
