# 스케줄 중앙화 TODO (정책 토글/cron 관리)

## 목표
- 모든 `@Scheduled` 잡을 batch 모듈로 집약하거나 batch가 위임 실행.
- 실행 여부·주기( cron/fixedDelay )를 Policy 모듈(yaml + UI 토글/cron)에서 통제.
- Policy 변경 시 `PolicyChangedEvent`를 통해 즉시 스케줄러 리프레시.

## 현황 (2025-11-24)
- batch: `AuditArchiveJob`, `AuditColdMaintenanceJob`
- audit: `AuditLogRetentionJob`, `AuditColdArchiveScheduler`, `RetentionCleanupJob`
- file-core: `FileScanRescheduler`, `FileAuditOutboxRelay`
- dw-ingestion-core: `DwIngestionOutboxRelay`
- draft: `OutboxDraftAuditRelay`
- 일부만 프로퍼티 기반이며, 정책/토글 UI 연동 없음.

## 모듈별 현황 & TODO (체크박스)

### batch 모듈
- [x] `AuditArchiveJob` (cron 프로퍼티) — 정책 토글 미연동
- [x] `AuditColdMaintenanceJob` (cron 프로퍼티) — 정책 토글 미연동
- [ ] 두 Job의 enable/cron을 PolicyToggleSettings로 위임, PolicyChangedEvent로 리프레시

### audit 모듈
- [x] `AuditPartitionScheduler` — PolicyChangedEvent 연동 완료
- [ ] `AuditLogRetentionJob` — cron 고정, 정책 토글/cron 미연동
- [ ] `AuditColdArchiveScheduler` — cron 고정, 정책 토글/cron 미연동
- [ ] `RetentionCleanupJob` — cron 고정, 정책 토글/cron 미연동
- [ ] 위 3개 Job을 batch로 이동하거나 batch에서 호출 + Policy 토글/cron 반영

### file-core 모듈
- [x] `FileScanRescheduler` — fixedDelay 프로퍼티
- [x] `FileAuditOutboxRelay` — fixedDelay 프로퍼티
- [ ] enable/fixedDelay를 Policy로 위임, batch에서 실행 위임 여부 결정

### dw-ingestion-core 모듈
- [x] `DwIngestionOutboxRelay` — fixedDelay 프로퍼티
- [ ] Policy 토글/interval 연동, batch 위임 여부 결정

### draft 모듈
- [x] `OutboxDraftAuditRelay` — fixedDelay 프로퍼티
- [ ] Policy 토글/interval 연동, batch 위임 여부 결정

## 해야 할 일 (체크리스트)
- [ ] PolicyToggleSettings에 각 잡별 enable/cron|fixedDelay 필드 추가
- [ ] Policy API/UI에서 편집 → PolicyChangedEvent(`security.policy`) 발행
- [ ] batch 쪽 스케줄러가 이벤트 수신 후 CronTrigger/fixedDelay 재설정
- [ ] yaml 기본값 유지(`audit.*`, `file.*`, `dw.*`, `draft.*`) + UI cron 편집 지원
- [ ] e2e: Policy API로 설정 변경 → 스케줄러 리프레시 → 실행 여부 확인

## 베스트 프랙티스 참고
- `AuditPartitionScheduler`: PolicyChangedEvent 수신 후 cron/enable/preloadMonths 즉시 반영.
- Alertmanager 스모크: CI 워크플로(`.github/workflows/alertmanager-smoke.yml`), 스크립트(`docs/monitoring/alertmanager-smoke.sh`).

## 진행 상태
- [x] 스케줄 현황 문서화
- [ ] 코드 이동/Policy UI 연동/테스트 미착수
