# OpenTelemetry & OTLP Guide (Consolidated)

## From: observability/opentelemetry-plan.md

# OpenTelemetry Tracing & Sampling Plan

## 1. 목표
- API, DW ingestion, batch, event relay 전체에 걸쳐 End-to-end trace 를 수집해 병목/오류를 가시화한다.
- Sampling 전략, Exporter 구성, 로그/메트릭과의 상관 trace ID 규칙을 정의한다.

## 2. 범위
- 모듈: `backend/server`, `backend/auth`, `backend/policy`, `backend/dw-gateway`, `backend/batch-app`.
- 구성요소: HTTP/gRPC, Spring Messaging, Quartz/batch, Outbox relay.

## 3. 아키텍처
1. **SDK & Instrumentation**
   - Spring Boot 3.3 + `spring-boot-starter-actuator` + `micrometer-tracing-bridge-otel`.
   - Auto-instrumentation (OpenTelemetry Java Agent) 적용, 커스텀 span 은 `@WithSpan` 으로 추가.
2. **Collector Topology**
   - Sidecar 혹은 Daemonset 형태의 OpenTelemetry Collector → OTLP gRPC 수집 → Prometheus/Grafana Tempo/Jaeger 로 export.
   - Prod: AWS Distro for OpenTelemetry (ADOT) + X-Ray/TEMPO 듀얼 export.
3. **Sampling 전략**
   - Default: 10% head sampling (parent-based). Latency > 500ms 혹은 오류 시 AlwaysOn.
   - 배치 작업: jobId 기준 deterministic sampling (traceId = hash(jobId)).
   - Feature flag 로 sampling 비율 조정 가능 (Config Server or Environment).
4. **Trace Context 연계**
   - Logs: `MDC.put("traceId", TraceContext.traceId())` → CloudWatch/Splunk 검색.
   - Metrics: Micrometer meter tags에 `trace.state` 추가.

## 4. 구현 단계
1. Gradle BOM 에 `opentelemetry-bom` 추가, `micrometer-tracing` 의존성 공유.
2. `platform` 모듈에 공통 `TracingConfig`(OTLP endpoint, sampling 설정) 추가.
3. Collector 배포: dev 환경 docker-compose (Collector + Tempo + Grafana), prod 는 ADOT.
4. DW ingestion/batch job 에 커스텀 span 추가 (`DwIngestionJob`, `OutboxRelay`).
5. Runbook 작성 (`docs/runbooks/tracing.md`), 대시보드에서 trace→log 연계 확인.

## 5. 모니터링/알람
- Missing span rate, exporter 실패율, collector queue length 를 Prometheus 로 추적.
- Sampling 비율 조정 시 SLO 영향 분석 (p95 latency, error budget).
- Alert: `trace_exporter_failures_total` 급증 시 on-call.

## 6. TODO
- [ ] Gradle libs 업데이트 및 의존성 추가.
- [ ] Collector 구성 템플릿/Helm 차트 작성.
- [ ] 앱별 `TracingConfig` + MDC 연계 구현.
- [ ] DW/batch 커스텀 span 삽입.
- [ ] Runbook/대시보드 작성.
\n---\n
## From: observability/otlp-internal.md

# OTLP 내부망 배포 가이드 (on-prem/폐쇄망)

## 목표
- 사내 수집기(예: 사내 OTEL Collector/Tempo/Jaeger)로 트레이스를 전송한다.
- 외부망 없이 동작하도록 엔드포인트, 인증, TLS(필요 시 사설 CA) 설정을 정리한다.

## 구성 예시
- Collector 주소: `http://otel-collector.infra.svc.cluster.local:4318/v1/traces`
- 인증: 내부망이면 기본 무인증, 보안 필요 시 Bearer 토큰/Basic 적용
- TLS: 사설 CA 사용 시 `OTEL_EXPORTER_OTLP_CERTIFICATE` 지정 또는 Collector TLS 검증 비활성(비권장)
- 샘플링: 초기 0.1 → 문제 발생 시 0.05로 조정, 디버깅 시 릴리스 전 일시 1.0

## 서비스별 설정 방법
- 프로파일: `SPRING_PROFILES_ACTIVE=otlp`
- 환경변수:
  - `OTEL_EXPORTER_OTLP_ENDPOINT=http://otel-collector.infra.svc.cluster.local:4318/v1/traces`
  - `OTEL_EXPORTER_OTLP_HEADERS=` (필요 시 `Authorization=Bearer <token>`)
  - `OTEL_RESOURCE_ATTRIBUTES=service.name=server,env=dev` (서비스마다 name 변경)
  - 샘플링: `MANAGEMENT_TRACING_SAMPLING_PROBABILITY=0.1`
- 로그 포맷: `logging.pattern.level=%5p trace_id=%X{traceId:-} span_id=%X{spanId:-}` (프로파일에 이미 포함)

## 검증 체크리스트
- Collector에서 span 수신 확인(Tempo/Jaeger UI 등)
- 서비스 로그에 trace_id/span_id 출력 확인
- 장애 시 재시도/백오프 여부(기본 OTel exporter 재시도 로직 사용)

## 운영 팁
- 배포 환경별 OTEL_RESOURCE_ATTRIBUTES에 `env`, `region`, `cluster` 태그를 포함해 대시보드에서 필터링.
- 샘플링을 낮춰도 특정 엔드포인트/사용자에 대한 “예외 샘플링”은 로그/메트릭으로 보완.
\n---\n
## From: observability/otlp-rollout.md

# OpenTelemetry OTLP 적용 가이드 (서버/DW/BATCH 공통)

## 1) 목표
- API/DW/배치 경로에 OTLP exporter 를 적용해 trace 기반 SLA(지연, 오류율) 관측을 가능하게 한다.
- Jaeger/Tempo/OTel Collector 등 백엔드로 전송, 필수 태그(tenant, userId, feature/action, jobId 등)를 포함한다.

## 2) 적용 범위
- `backend/server` (REST)
- `backend/dw-gateway` (DW REST)
- `backend/dw-worker` / `batch-app` (배치/워커)

## 3) Gradle/의존성
- (이미 BOM 사용) `implementation("io.opentelemetry:opentelemetry-exporter-otlp")`
- Spring Boot 3.3 → Micrometer Tracing 자동설정 활용 가능.

## 4) 기본 설정 예시 (`application.yaml`)
```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 0.1   # 초기 10% 샘플링, SLA 구간 관측 후 조정
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT:http://otel-collector:4318/v1/traces}
      headers:
        Authorization: ${OTEL_EXPORTER_OTLP_HEADERS:}
```

## 5) 필수 Tag/Span customization
- Web ingress: `http.method`, `http.route`, `feature`, `action`, `principalId`.
- DW ingestion: `jobId`(Outbox UUID), `jobType`, `feedType`, `fileName`.
- Batch: `trigger`(schedule/manual), `rowScope`.
- 구현 포인트: `ObservationHandler` 또는 `Tracer` 커스터마이저 빈.

## 6) 배포/런타임 변수
- `OTEL_EXPORTER_OTLP_ENDPOINT` (필수)
- `OTEL_SERVICE_NAME` (`server`, `dw-gateway`, `dw-worker`, `batch-app`)
- `OTEL_EXPORTER_OTLP_HEADERS` (선택, 예: `Authorization=Bearer ...`)
- `OTEL_RESOURCE_ATTRIBUTES` (예: `deployment.environment=prod,region=ap-northeast-2`)

## 7) 대시보드 초안 (Tempo/Grafana)
- API p95 latency by route/feature
- Ingestion pipeline latency (outbox -> worker)
- Error rate by feature/action
- Top slow DB queries (trace spans)

## 8) 단계별 롤아웃
1. Dev 환경: 샘플링 100%로 단기 수집, 필수 tag 검증.
2. Stage: 샘플링 10~20%, 대시보드 생성 및 알람 임계치 설정.
3. Prod: 샘플링 5~10%로 시작, 이상 징후 시 점진 확대. 롤백은 환경변수로 비활성화.

## 9) 테스트 전략
- `@SpringBootTest` + WireMock 입력, `OtlpGrpcServer`(testcontainers)로 span 수신 검증.
- 배치/워커: 통합테스트에서 `InMemorySpanExporter` 로 span 수집 후 tag/assert 확인.
