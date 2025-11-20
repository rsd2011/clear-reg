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
