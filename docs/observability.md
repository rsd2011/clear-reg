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
