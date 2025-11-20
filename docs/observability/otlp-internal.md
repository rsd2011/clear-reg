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
