# Architecture Improvement TODO Plan

## Context
- Base stack: Java 21, Spring Boot 3.3, Gradle multi-module. Modules: platform, auth, dw-integration, policy, draft, server, batch-app.
- Key drivers: permission enforcement, DW ingestion, caching, ops automation.
- Constraints: small team, focus on g성보안/데이터 거버넌스, avoid regressions.

## Guiding Principles
1. Favor modular boundaries (auth/policy/dw) with clear interfaces before splitting services.
2. Keep `docs/permissions.md` and DW ingestion doc as source of truth; update alongside any change.
3. Every backlog item must map to measurable outcomes (coverage, latency, deployment uptime, etc.).

## 2025-11-19 Architecture Review Snapshot
- 강점: 모듈 간 포트 구조 정립, 권한/RowScope 파이프라인 표준화, CI 품질 게이트와 Actuator/Prometheus 기반 관측성 확보.
- 주요 리스크: DW 파이프라인 decoupling 미완성(`dw-worker` 큐/재시도), CQRS read-model 미구현, 토큰/세션 회수 전략 공백, GitOps·롤백 자동화가 문서 수준에 머무름.
- 권장 액션: 단기적으로 dw-worker Outbox 연계·테스트 보강, Redis read-model 구축, 토큰 블랙리스트/ArchUnit 규칙 추가, GitOps 파이프라인 MVP 구성.
- 중기 이후: SQS/Kafka 전환, read-model 전면 전환, 중앙 캐시 무효화 채널 실제 구현, OpenTelemetry·Chaos 테스트 실 적용.

## TODO Backlog

### A. Governance & Permissions (Weeks 1-6)
- [x] Add schema validation + CI lint for `permission-groups.yml` and other declarative configs (owner: auth team).
- [x] Introduce GitOps-style change flow: PR template, diff preview, hash logging for permission/policy bundles.
- [x] Implement repository-layer RowScope predicates (Querydsl specs) and enforce via ArchUnit tests.
- [x] Provide `AuthContext` propagation utility for `@Async`, Quartz, and batch jobs; add smoke test coverage.

### B. Module Decoupling & Interfaces (Weeks 2-8)
- [x] Define interfaces (ports) for policy, file, DW access used by `backend/server`; refactor controllers to ports.
  - [x] Policy administration port introduced (`PolicyAdminPort`).
  - [x] DW ingestion policy port introduced (`DwIngestionPolicyPort`).
  - [x] DW batch query port introduced (`DwBatchPort`).
  - [x] DW organization query port introduced (`DwOrganizationPort`).
  - [x] File management port introduced (`FileManagementPort`).
    - [x] 파일 DTO/사양 문서화 (`docs/file/file-api-plan.md`), 포트가 `FileMetadataDto` 기반으로 변경됨.
- [x] Add contract tests ensuring ports remain backward-compatible.
  - [x] Policy, DW ingestion policy, and DW batch ports now covered by adapter tests.
- [x] Audit `backend/server` for direct entity cross-usage; replace with DTOs or service calls.
  - [x] Completed dependency audit (`docs/module-decoupling/server-audit.md`); server now uses only port DTOs.
ㅔ- [x] Port 호환성 회귀 방지를 위해 `backend/dw-gateway-client` 기반 MockServer/contract 테스트 보강 (policy/file/DW 전 포트 대상).

### C. Data & DW Pipeline (Weeks 4-10)
- [x] Separate DW ingestion REST API from main server (new Spring Boot app or dedicated profile) and align with batch workers.
  - [x] Created `backend/dw-gateway` module hosting DW batch/policy controllers and ports.
  - [x] DW gateway/worker 연동 및 클라이언트 구성을 문서화 (`docs/dw/dw-gateway-overview.md`).
- [x] Introduce queue/outbox abstraction for ingestion tasks (Kafka/SQS placeholder) with idempotency checks.
  - [x] Added `DwIngestionJobQueue` abstraction, outbox relay plan (`docs/dw/outbox-plan.md`) describing SQS/Kafka migration steps.
  - [x] Outbox 엔티티에 payload/lock 메타 데이터 추가, 비관적 락 기반 클레임 및 Broker Publisher 포트(No-op) 준비.
  - [x] Kafka 퍼블리셔/컨슈머(플래그 기반) 구현, 표준 페이로드 스키마(`DwIngestionOutboxEvent`) 확정. SQS 프로바이더는 후속.
- [x] Review transactional boundaries between DW snapshot tables and operational tables; document commit order and rollback procedures.
  - [x] Documented current transaction/rollback flow in `docs/dw/transaction-boundaries.md`.
- [x] Optimize query layer: enable Hibernate batch fetch + DTO projections where RowScope filters cannot rely on cache alone.
  - [x] File listing now uses projection (`FileSummaryView`) and `@BatchSize` on versions to limit N+1 access.
- [x] `dw-worker`에서 Outbox → Ingestion 서비스 연계를 구현하고 재시도/Dead-letter 정책을 코드로 반영 (참고: `docs/dw/dw-worker-plan.md`).
- [x] Transactional outbox & DW snapshot 동시성 검증용 통합테스트 추가 (`dw-ingestion-core`, `dw-worker` 모듈 대상).

### D. Observability & Operations (Weeks 1-12)
- [x] Enable Spring Boot Actuator across modules; expose Prometheus metrics and health groups.
- [x] Create Grafana dashboard (auth errors, DW ingestion throughput, cache evictions, permission denials).
- [x] Define alert thresholds and runbook entries (docs/runbooks/*) for ingestion failure, cache saturation, token errors.
- [x] Integrate CI/CD pipeline (GitHub Actions/Jenkins) with Jacoco gate and `./gradlew test jacocoTestCoverageVerification` default target.

### E. Medium-term Initiatives (3-12 Months)
- [x] Split DW ingestion into `dw-gateway` (REST) and `dw-worker` (batch) with asynchronous queue and retry policy.
  - [x] dw-worker 분리/큐 설계 문서화 (`docs/dw/dw-worker-plan.md`).
  - [x] Outbox 폴러 + Kafka 연동(플래그 기반) 구현, JSON 스키마 표준화. SQS 프로바이더는 후속.
  - [x] `dw-worker` 컨테이너라이징 및 독립 스케일링 파이프라인 설계/매니페스트 추가 (`docs/dw/dw-worker-deploy.md`, Dockerfile 포함).
- [ ] Materialize read models (Redis/Elasticache) for organization tree, menu, and permission menus (CQRS pattern).
  - [x] 설계/로드맵 문서화 완료 (`docs/data/read-model-plan.md`).
  - [x] Redis/Elasticache 스키마 + TTL/무효화 정책 구현, API fallback 시나리오 검증 (`docs/data/organization-read-model.md`).
  - [x] RowScope/권한 변경 이벤트 기반 read-model 갱신 파이프라인 및 회귀 테스트 작성 (PermissionSetChanged → read model rebuild).
- [ ] Adopt GitOps-managed policy repo; automate deployment + rollback of YAML changes.
  - [x] GitOps 저장소/파이프라인 설계 문서화 (`docs/policy/gitops-repo-plan.md`).
  - [x] 정책 repo → Jenkins/GitHub Actions 자동 검증 + diff 시각화 + 승인 플로우 구현 (`.github/workflows/policy-gitops.yml`, `docs/policy/gitops-pipeline.md`).
  - [x] 실패 시 자동 롤백/알림 시나리오 테스트 (workflow_dispatch rollback + 공용 배포 스크립트).
- [ ] Implement centralized cache invalidation channel (Redis pub/sub) for RowScope/org caches.
  - [x] 설계안/전파 플로우 문서화 (`docs/data/cache-invalidation-plan.md`).
  - [x] Redis pub/sub 기반 캐시 무효화 채널 MVP 구현, server에 적용 (구독/핸들러/퍼블리셔). dw-worker/batch는 후속 적용.
- [ ] Define CI-managed database migration strategy (Flyway/Liquibase) including rollback verification.
  - [x] Flyway 기반 CI/rollback 전략 문서화 (`docs/data/db-migration-ci-plan.md`).
  - [x] Rollback 시뮬레이션을 CI 파이프라인에 추가 (`.github/workflows/db-migration.yml`, `scripts/verify-migrations.sh`). 주요 모듈 smoke 테스트는 후속.

### F. Long-term Vision (>12 Months)
- [ ] Gradually peel off bounded contexts (Auth, Policy, File, DW Query) into independent Spring Boot services or Modulith slices with separate scaling policies.
  - [x] 장기 분리 전략/단계별 계획 수립 (`docs/architecture/bounded-context-strategy.md`).
- [ ] Add domain event bus (Kafka/EventBridge) for permission change, DW batch completion, file policy updates driving cache clear/audit flows.
  - [x] 이벤트 버스 도입 계획 수립 (`docs/architecture/domain-event-bus-plan.md`).
- [ ] Introduce OpenTelemetry tracing + sampling strategy covering API, DW ingestion, batch.
  - [x] Trace/Sampling 설계 문서화 (`docs/observability/opentelemetry-plan.md`).
  - [x] 실제 서비스/배치 경로에 OTLP exporter 적용 가이드 작성 (`docs/observability/otlp-rollout.md`), 환경변수/샘플링/대시보드 초안 포함. 코드 반영은 후속.
- [ ] Invest in resilience testing (chaos experiments) and zero-downtime deploy tooling (Argo Rollouts/Spinnaker).
  - [x] Chaos/무중단 배포 계획 수립 (`docs/observability/resilience-testing-plan.md`).
  - [x] Argo Rollouts/Blue-Green PoC 계획 수립 (`docs/observability/argo-rollouts-poc.md`), Prometheus 연동·슬랙 알림 포함. 실행은 후속.

## Tracking & Review
- Weekly architecture sync reviews progress on sections A-D.
- Monthly steering meeting evaluates readiness to start section E tasks.
- Document updates: link PRs to this TODO file and related docs (`docs/permissions.md`, `backend/dw-integration/DW_INGESTION.md`).
