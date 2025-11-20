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
