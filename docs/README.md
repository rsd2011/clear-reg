# Docs Index (post-refactor)

## Top-level map
- overview/roadmap.md — 상위 로드맵, ADR은 overview/adr/
- architecture/ — 경계/이벤트/모듈 디커플링 설계
- security/permissions/ — 권한·데이터 정책 SSOT + GitOps + 해시 로그
- security/policy/ — 정책 GitOps 저장소/파이프라인
- data/ — read-model, DW, file, DB migration CI
- operations/observability/ — 관측/알림/OTEL/런북/롤아웃
- operations/siem/ — SIEM 페이로드/파이프라인
- audit/ — 감사 모듈 설계·E2E·런북
- drafts/ — 기안/승인 관련 TODO·예제·API 초안
- approvals/ — 결재 연동 문서
- schedules/ — 스케줄 중앙화 TODO
- migrations/ — SQL 및 rollback-log
- quality/ — 테스트 규칙, CI 요약, 커버리지 임계
- monitoring-artifacts/ — Alertmanager 룰/스모크 스크립트

## Quick links
- 권한/마스킹/RowScope: `security/permissions/permissions.md`
- 권한 GitOps 절차: `security/permissions/gitops.md`, 해시: `security/permissions/bundle-digests.json`
- Alertmanager 가이드: `operations/observability/alerting.md`, 룰 파일: `monitoring-artifacts/audit-alerts.yml`
- OTEL/Trace: `operations/observability/otel.md`
- Read Model: `data/read-model/overview.md`
- DW Worker/Outbox: `data/dw/worker.md`, `data/dw/outbox-plan.md`
