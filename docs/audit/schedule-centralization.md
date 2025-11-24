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

## 모듈별 해야 할 일 (체크박스)

### batch
- [x] 현행 Job: `AuditArchiveJob`, `AuditColdMaintenanceJob` (cron 프로퍼티 기반)
- [ ] PolicyToggleSettings에 enable/cron 연결 → PolicyChangedEvent 수신 시 재스케줄 (루즈 커플링 설계 반영)
- [ ] 타 모듈 Job 위임 실행 구조 마련 (중앙 스케줄러 패턴 적용)

### audit
- [x] `AuditPartitionScheduler` (PolicyChangedEvent 연동 완료)
- [ ] `AuditLogRetentionJob` cron을 Policy enable/cron으로 이관 (중앙 스케줄러 위임)
- [ ] `AuditColdArchiveScheduler` cron을 Policy enable/cron으로 이관 (중앙 스케줄러 위임)
- [ ] `RetentionCleanupJob` cron을 Policy enable/cron으로 이관 (중앙 스케줄러 위임)
- [ ] 필요 시 위 Job을 batch로 이동/위임

### file-core
- [x] `FileScanRescheduler` fixedDelay 프로퍼티
- [x] `FileAuditOutboxRelay` fixedDelay 프로퍼티
- [ ] enable/fixedDelay를 Policy로 노출, batch 위임 여부 결정 (중앙 스케줄러 패턴 적용)

### dw-ingestion-core
- [x] `DwIngestionOutboxRelay` fixedDelay 프로퍼티
- [ ] Policy 토글/interval 연동, batch 위임 여부 결정 (중앙 스케줄러 패턴 적용)

### draft
- [x] `OutboxDraftAuditRelay` fixedDelay 프로퍼티
- [ ] Policy 토글/interval 연동, batch 위임 여부 결정 (중앙 스케줄러 패턴 적용)

## 공통 작업
- [ ] PolicyToggleSettings 확장: 잡별 enable + cron|fixedDelay(+timeUnit)
- [ ] Policy API/UI 편집 → PolicyChangedEvent 발행 → 스케줄러 재설정
- [ ] yaml 기본값 유지(`audit.*`, `file.*`, `dw.*`, `draft.*`) + UI cron 편집 지원
- [ ] e2e: Policy API 변경 → Event → 실행 여부 검증

## 베스트 프랙티스 참고
- `AuditPartitionScheduler`: PolicyChangedEvent 수신 후 cron/enable/preloadMonths 즉시 반영 패턴 재사용.
- Alertmanager 스모크: CI 워크플로(`.github/workflows/alertmanager-smoke.yml`), 스크립트(`docs/monitoring/alertmanager-smoke.sh`).

## 루즈 커플링 설계안 (Spring 기반, 브로커 없이)
- 중앙 스케줄러(Runner)는 batch 모듈에만 존재하고, 각 업무 모듈은 “실행 포트”만 노출한다.
  - `ScheduledJobPort` (interface) 예: `runOnce(Instant now)` 메서드.
  - batch에서 정책을 읽어 활성화된 Job 목록을 동적으로 등록( `SchedulingConfigurer` + `ScheduledTaskRegistrar` ).
- 정책 동기화 흐름
  1) Policy 서비스: UI/yaml → Policy API → `PolicyChangedEvent(security.policy)` 발행
  2) batch: 이벤트 수신 → 활성화 Job/cron/fixedDelay 재계산 → `ScheduledTaskRegistrar` 재등록
  3) 실패 시 fallback: yml 기본값으로 안전 실행
- 상태/모니터링
  - 각 Job 실행 전후 Micrometer Timer/Counter 기록 → Prometheus → Alertmanager 룰 재사용.
  - 마지막 실행시각/결과는 Job 포트에서 반환하거나 별도 `job_execution` 테이블에 적재.
- 실행 격리
  - 무거운 Job은 `@Async` + 전용 `TaskExecutor`로 분리해 web thread 영향 최소화.
  - 필요 시 Job 단위 timeout/fail-fast 적용 (`Future.get(timeout)`).
- 구성 관리
  - 모든 스케줄 설정은 PolicyToggleSettings + yml 기본값으로 통일, 코드 하드코딩 금지.
  - UI에서 cron 표현식을 직접 입력, 미입력 시 기본값 사용.


## 진행 상태
- [x] 현황/계획 문서화
- [ ] 코드 이동·Policy UI 연동·테스트 미착수
