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

## 해야 할 일
1) 모듈 정리
   - 각 모듈의 잡을 batch 쪽으로 이동하거나 batch에서 Scheduler로 위임.
2) Policy 스키마 확장
   - `PolicyToggleSettings`에 각 잡별 `enabled`, `cron|fixedDelay`, `timeUnit` 필드 추가.
   - Policy API/UI에서 수정 → `PolicyChangedEvent(code="security.policy")` 발행.
3) 스케줄러 리프레시
   - batch에서 이벤트 리스너로 `CronTrigger` 재생성, fixedDelay 재설정.
4) 기본값(yaml)
   - `audit.*`, `file.*`, `dw.*`, `draft.*`에 보수적 기본 cron/fixedDelay 유지.
5) 테스트/e2e
   - Policy API로 cron/enable 변경 → 이벤트 발행 → 다음 실행이 새 설정을 따르는지 검증.
   - Playwright/통합 테스트: UI 토글 + cron 입력 → 실행/비활성화 확인.

## 베스트 프랙티스 참고
- `AuditPartitionScheduler`가 PolicyChangedEvent 수신 후 cron/enable/preloadMonths를 즉시 반영하는 패턴 재사용.
- Alertmanager 룰/배치 스모크: CI 워크플로(`.github/workflows/alertmanager-smoke.yml`) + 스크립트(`docs/monitoring/alertmanager-smoke.sh`).

## 진행 상태
- 수집/정리 문서화만 완료. 실제 코드 이동·Policy UI 연동·테스트는 미착수.
